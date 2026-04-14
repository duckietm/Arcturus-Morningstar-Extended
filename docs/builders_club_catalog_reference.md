# Builders Club Catalog Reference

Questa guida riassume il setup corretto dopo la separazione completa tra catalogo normale e `Builders Club`.

## Tabelle usate davvero

- Catalogo normale:
  - `catalog_pages`
  - `catalog_items`
- Builders Club:
  - `catalog_pages_bc`
  - `catalog_items_bc`
- Abbonamenti / add-on BC venduti nel catalogo normale:
  - `catalog_club_offers`

Quindi:

- se vuoi una pagina BC, va in `catalog_pages_bc`
- se vuoi un furni BC, va in `catalog_items_bc`
- se vuoi vendere lo stesso furni anche nel catalogo normale, aggiungi un'altra riga normale in `catalog_items`

Questo è proprio il vantaggio della separazione: lo stesso `item_id` può comparire sia nel catalogo normale sia nel BC, ma con comportamenti diversi.

## Differenza pratica tra catalogo normale e BC

### Catalogo normale

- gli offer arrivano da `catalog_items`
- hanno costi normali (`cost_credits`, `cost_points`, ecc.)
- quando comprati diventano proprietà utente / inventario

### Builders Club

- gli offer arrivano da `catalog_items_bc`
- non hanno prezzo perché il piazzamento BC usa il flow dedicato
- non entrano nell'inventario utente
- non diventano mai proprietà utente
- quando rimossi dalla stanza vengono eliminati

## Migration da applicare

Assicurati di avere applicato:

- `Database Updates/009_add_builders_club_catalog_offers.sql`
- `Database Updates/010_add_catalog_mode_to_catalog_pages.sql`
- `Database Updates/011_add_builders_club_trial_room_lock.sql`
- `Database Updates/012_support_builders_club_catalog_tables.sql`

La `012` è importante perché aggiorna `catalog_pages_bc.page_layout` con i layout BC moderni:

- `builders_club_frontpage`
- `builders_club_addons`
- `builders_club_loyalty`

## Come aggiungere pagine BC

Le pagine BC vanno create in `catalog_pages_bc`.

Esempio:

```sql
INSERT INTO catalog_pages_bc
(
    parent_id,
    caption,
    page_layout,
    icon_color,
    icon_image,
    order_num,
    visible,
    enabled,
    page_headline,
    page_teaser,
    page_special,
    page_text1,
    page_text2,
    page_text_details,
    page_text_teaser
)
VALUES
(
    -1,
    'Builders Furni',
    'default_3x3',
    1,
    28,
    1,
    '1',
    '1',
    'catalog_header_roombuilder',
    '',
    '',
    'Builders Club',
    'Linea test',
    'Pagina test del Builders Club',
    ''
);
```

## Come aggiungere furni BC

I furni BC vanno in `catalog_items_bc`.

Esempio:

```sql
INSERT INTO catalog_items_bc
(
    item_ids,
    page_id,
    catalog_name,
    order_number,
    extradata
)
VALUES
(
    '12345',
    1,
    'bc_test_sofa',
    1,
    ''
);
```

Dove:

- `item_ids` = ID del base item
- `page_id` = ID pagina in `catalog_pages_bc`
- `catalog_name` = chiave offer/localization

## Come vendere lo stesso furni anche nel catalogo normale

Se vuoi che lo stesso furni sia:

- vendibile nel catalogo normale
- disponibile anche nel Builders Club

devi avere **due righe distinte**:

### Normale

```sql
INSERT INTO catalog_items
(
    page_id,
    item_ids,
    catalog_name,
    cost_credits,
    cost_points,
    points_type,
    amount,
    club_only,
    extradata,
    have_offer,
    offer_id,
    limited_stack,
    order_number
)
VALUES
(
    500,
    '12345',
    'normal_test_sofa',
    5,
    0,
    0,
    1,
    '0',
    '',
    '1',
    -1,
    0,
    1
);
```

### Builders Club

```sql
INSERT INTO catalog_items_bc
(
    item_ids,
    page_id,
    catalog_name,
    order_number,
    extradata
)
VALUES
(
    '12345',
    1,
    'bc_test_sofa',
    1,
    ''
);
```

Quindi lo stesso base item `12345` può vivere in entrambi i cataloghi senza condividere il prezzo.

## Abbonamento e add-on BC

Abbonamento e add-on non stanno in `catalog_items_bc`.

Vanno in:

- `catalog_club_offers`

Tipi supportati:

- `BUILDERS_CLUB`
- `BUILDERS_CLUB_ADDON`

Sono venduti nel catalogo normale, come HC/VIP, ma il widget BC usa comunque le sue pagine dedicate da `catalog_pages_bc`.

## Nota su `catalog_mode`

`catalog_mode` resta nella tabella `catalog_pages`, ma non è più il meccanismo principale per far comparire le pagine nel Builders Club.

Adesso il runtime BC legge direttamente:

- `catalog_pages_bc`
- `catalog_items_bc`

Quindi:

- aggiungere pagine BC in `catalog_pages` non basta
- aggiungere items BC in `catalog_items` non basta
- usare `BOTH` su una pagina normale non la renderà automaticamente una pagina BC

## Query utili per test

### Elencare pagine BC

```sql
SELECT * FROM catalog_pages_bc ORDER BY parent_id, order_num, id;
```

### Elencare items BC

```sql
SELECT * FROM catalog_items_bc ORDER BY page_id, order_number, id;
```

### Trovare lo stesso furni in entrambi i cataloghi

```sql
SELECT 'NORMAL' AS source, id, page_id, item_ids, catalog_name
FROM catalog_items
WHERE item_ids = '12345'

UNION ALL

SELECT 'BC' AS source, id, page_id, item_ids, catalog_name
FROM catalog_items_bc
WHERE item_ids = '12345';
```

## Consiglio pratico

Per fare test rapidi:

1. crea una pagina in `catalog_pages_bc`
2. inserisci 1-2 furni in `catalog_items_bc`
3. lascia gli stessi furni anche in `catalog_items` se li vuoi vendibili normalmente
4. pubblica / ricarica il catalogo

Se vuoi, possiamo aggiungere anche un file SQL separato con qualche pagina BC e qualche furni BC già pronti da importare per i test.

## Seed demo già pronto

Se vuoi una demo immediata, puoi usare:

- `Database Updates/013_seed_builders_club_sample_page.sql`

Questo seed:

- crea una root BC demo
- crea una pagina BC demo figlia
- duplica alcuni furni già esistenti del catalogo normale dentro `catalog_items_bc`

Così puoi testare subito il caso:

- stesso furni vendibile nel catalogo normale
- stesso furni disponibile anche nel Builders Club
