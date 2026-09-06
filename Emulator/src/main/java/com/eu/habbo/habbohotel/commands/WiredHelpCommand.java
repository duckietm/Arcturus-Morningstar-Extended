package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.gameclients.GameClient;

/** Shows only the text placeholders implemented by this hotel. */
public class WiredHelpCommand extends Command {
    public WiredHelpCommand() {
        super(null, new String[] {"wiredhelp", "aiutowired"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        gameClient.getHabbo().alert(
                "<b>Guida WIRED - testo dinamico</b>\r"
                        + "Queste variabili funzionano solo se sulla casella del WIRED Messaggio hai posato il relativo Extra.\r\r"
                        + "<b>Variabili pronte:</b>\r"
                        + "%username% - nome dell'utente che ha attivato il WIRED\r"
                        + "%roomname% - nome della stanza\r"
                        + "%owner% - proprietario della stanza\r"
                        + "%user_count% - utenti presenti nella stanza\r"
                        + "%online_count% - utenti online nell'hotel\r"
                        + "%item_count% - numero di furni selezionati\r"
                        + "%name% - nome del bot/furni che invia il testo\r\r"
                        + "<b>Esempio caffetteria</b>\r"
                        + "Benvenuto %username%!\rCi sono %online_count% utenti online.\r\r"
                        + "Per variabili numeriche personalizzate usa gli Extra Variabile utente, stanza o furni e inserisci nel testo il token configurato nell'Extra.");
        return true;
    }
}
