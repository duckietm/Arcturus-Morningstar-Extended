import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const configPath = path.join(root, 'config.ini');
const outputPath = path.join(root, 'src', 'main', 'resources', 'db', 'migration', 'V20260810153000__italian_commands_and_help.sql');
const cachePath = path.join(root, 'scripts', '.command-translation-cache.json');

const config = {};
for (const rawLine of fs.readFileSync(configPath, 'utf8').split(/\r?\n/)) {
    const match = rawLine.match(/^\s*([^#;][^=]+?)\s*=\s*(.*?)\s*$/);
    if (match) config[match[1].trim()] = match[2].trim();
}

const mysqlCandidates = [
    'C:\\Program Files\\MariaDB 12.3\\bin\\mysql.exe',
    'C:\\Program Files\\MariaDB 11.8\\bin\\mysql.exe',
    'C:\\Program Files\\MariaDB 11.7\\bin\\mysql.exe',
    'C:\\Program Files\\MariaDB 11.6\\bin\\mysql.exe',
    'C:\\xampp\\mysql\\bin\\mysql.exe'
];
const mysql = mysqlCandidates.find(candidate => fs.existsSync(candidate));
if (!mysql) throw new Error('mysql.exe non trovato');

const database = config['db.database'] || 'polaris_local';
const username = config['db.username'] || 'root';
const rowsText = execFileSync(mysql, [
    '-h', config['db.host'] || '127.0.0.1',
    '-P', config['db.port'] || '3306',
    '-u', username,
    '-D', database,
    '--default-character-set=utf8mb4',
    '-N', '-B', '--raw',
    '-e', "SELECT JSON_OBJECT('key', `key`, 'value', value) FROM emulator_texts WHERE `key` LIKE 'commands.%' ORDER BY `key`"
], {
    cwd: root,
    encoding: 'utf8',
    env: { ...process.env, MYSQL_PWD: config['db.password'] || '' },
    maxBuffer: 16 * 1024 * 1024
});

const rows = rowsText.split(/\r?\n/).filter(Boolean).map(line => JSON.parse(line));
const cache = fs.existsSync(cachePath) ? JSON.parse(fs.readFileSync(cachePath, 'utf8')) : {};

const protect = value => {
    const tokens = [];
    const text = value.replace(
        /%[^%\s]+%|https?:\/\/\S+|<[^<>\r\n]+>|\{[^{}\r\n]+}|:[a-zA-Z0-9_]+|<br\s*\/?>|\\r|\\n/gi,
        match => {
            const token = `__CMDTOKEN_${tokens.length}__`;
            tokens.push(match);
            return token;
        }
    );
    return { text, tokens };
};

const restore = (value, tokens) => tokens.reduce(
    (text, token, index) => text.replace(new RegExp(`__CMDTOKEN[_ ]?${index}__`, 'gi'), token),
    value
);

const translate = async value => {
    if (!/[A-Za-z]{2}/.test(value)) return value;
    if (cache[value]) return cache[value];

    const { text, tokens } = protect(value);
    const url = new URL('https://translate.googleapis.com/translate_a/single');
    url.searchParams.set('client', 'gtx');
    url.searchParams.set('sl', 'auto');
    url.searchParams.set('tl', 'it');
    url.searchParams.set('dt', 't');
    url.searchParams.set('q', text);

    let response;
    for (let attempt = 0; attempt < 5; attempt++) {
        response = await fetch(url, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        if (response.ok) break;
        await new Promise(resolve => setTimeout(resolve, 400 * (attempt + 1)));
    }
    if (!response?.ok) throw new Error(`Traduzione fallita: HTTP ${response?.status}`);

    const payload = await response.json();
    const translated = restore((payload?.[0] || []).map(part => part?.[0] || '').join(''), tokens).trim() || value;
    cache[value] = translated;
    return translated;
};

let cursor = 0;
const localized = new Array(rows.length);
const worker = async () => {
    while (cursor < rows.length) {
        const index = cursor++;
        const row = rows[index];
        localized[index] = {
            key: row.key,
            value: row.key.startsWith('commands.keys.') ? row.value : await translate(String(row.value))
        };
        if ((index + 1) % 40 === 0) process.stdout.write(`Tradotte ${index + 1}/${rows.length}\n`);
    }
};

await Promise.all(Array.from({ length: 6 }, worker));
fs.writeFileSync(cachePath, JSON.stringify(cache, null, 2) + '\n', 'utf8');

const sqlString = value => `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")}'`;
const values = localized.map(row => `    (${sqlString(row.key)}, ${sqlString(row.value)})`).join(',\n');
const helpTexts = [
    ['commands.keys.cmd_commands', 'commands;cmds;help;aiuto;comandi'],
    ['commands.description.cmd_commands', ':help [pagina] - Mostra i comandi disponibili per il tuo rank.'],
    ['commands.generic.cmd_commands.text', 'Comandi disponibili'],
    ['commands.generic.cmd_commands.page', 'Pagina %page% di %pages% - %count% comandi'],
    ['commands.generic.cmd_commands.hint', 'Usa :help <pagina> per continuare.'],
    ['commands.error.cmd_commands.invalid_page', 'Pagina non valida. Scegli un numero da 1 a %pages%.'],
    ['commands.generic.cmd_commands.empty', 'Non hai comandi disponibili.']
].map(([key, value]) => `    (${sqlString(key)}, ${sqlString(value)})`).join(',\n');

const sql = `-- Localizzazione italiana completa dei comandi. Alias, placeholder e sintassi sono preservati.\n` +
`INSERT INTO \`emulator_texts\` (\`key\`, \`value\`) VALUES\n${values}\n` +
`ON DUPLICATE KEY UPDATE \`value\` = VALUES(\`value\`);\n\n` +
`-- Lista comandi paginata e alias help italiani.\n` +
`INSERT INTO \`emulator_texts\` (\`key\`, \`value\`) VALUES\n${helpTexts}\n` +
`ON DUPLICATE KEY UPDATE \`value\` = VALUES(\`value\`);\n`;

fs.writeFileSync(outputPath, sql, 'utf8');
process.stdout.write(`Scritta migrazione con ${localized.length} stringhe: ${outputPath}\n`);
