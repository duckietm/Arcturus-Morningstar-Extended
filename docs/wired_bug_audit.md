# Wired Bug Audit

## 1. Scopo

Questo documento raccoglie i **potenziali bug**, le **aree fragili** e le **incoerenze architetturali** emerse durante l’analisi del sistema wired.

Non tutti i punti qui sotto sono bug già riprodotti al 100%, ma sono:

- problemi già visti in comportamento reale
- incongruenze tra runtime e UI
- zone del codice che possono generare regressioni o risultati non deterministici

Riferimenti principali:

- `Arcturus-Morningstar-Extended/Emulator/src/main/java/com/eu/habbo/habbohotel/wired/core/WiredManager.java`
- `Arcturus-Morningstar-Extended/Emulator/src/main/java/com/eu/habbo/habbohotel/wired/core/WiredEngine.java`
- `Arcturus-Morningstar-Extended/Emulator/src/main/java/com/eu/habbo/habbohotel/wired/WiredHandler.java`
- `Arcturus-Morningstar-Extended/Emulator/src/main/java/com/eu/habbo/habbohotel/wired/core/WiredMoveCarryHelper.java`
- `Arcturus-Morningstar-Extended/Emulator/src/main/java/com/eu/habbo/habbohotel/items/interactions/wired`

---

## 2. Sintesi Priorità

| Priorità | Tema | Stato |
|---|---|---|
| Alta | `context` variabili esposto ma non implementato davvero | Incoerenza forte |
| Alta | Doppio runtime (`WiredManager` vs `WiredHandler`) | Rischio architetturale |
| Alta | Ordine effect non sempre garantito senza extra esplicito | Rischio comportamentale |
| Alta | Path movimento legacy può ancora far trapelare update intermedi | Già osservato in stanza |
| Media | Tick a `50ms` ma delay wired in step da `500ms` | Semantica non uniforme |
| Media | Polling realtime `:wired` a `50ms` | Rischio carico/runtime noise |
| Media | `click furni` ora immediato, queue/cancel svuotati | Possibile regressione |
| Media | Semantica timestamp variabili non uniforme tra target types | Possibile confusione logica |

---

## 3. Audit Dettagliato

## 3.1 `context` nelle variabili: esposto ma non veramente supportato

- **Gravità:** Alta
- **Confidenza:** Alta
- **Area:** effetti/condizioni/extras variabili

### Problema

Nel layout e in parte della serializzazione compare il target `context`, ma in più punti il runtime lo rifiuta esplicitamente oppure restituisce direttamente `false`.

Questo crea una situazione pericolosa:

- il designer pensa che la feature esista
- il box si salva o si configura parzialmente
- ma poi in esecuzione non produce il comportamento atteso

### Evidenze

- `WiredEffectGiveVariable.java:197`
  - il save rifiuta `TARGET_CONTEXT`
- `WiredConditionVariableValueMatch.java:181`
  - `case TARGET_CONTEXT -> false`
- `WiredConditionVariableAgeMatch.java:146`
  - `case TARGET_CONTEXT -> false`
- `WiredExtraTextOutputVariable.java:83`
  - il save rifiuta `TARGET_CONTEXT`

### Impatto pratico

- stack che sembrano validi in UI ma non funzionano a runtime
- falsi negativi nelle condition variabili
- placeholder testuali variabili non disponibili quando l’utente si aspetta il target context

### Fix suggerito

Scegliere una direzione netta:

1. **o** implementare davvero `context` in tutti i flow variabili
2. **o** rimuoverlo completamente da UI, save e runtime finché non è pronto

La seconda opzione è la più sicura nel breve periodo.

---

## 3.2 Doppio runtime wired ancora presente

- **Gravità:** Alta
- **Confidenza:** Alta
- **Area:** architettura core

### Problema

`WiredManager` dichiara di essere il runtime esclusivo e tratta i vecchi flag come sola compatibilità, ma `WiredHandler` esiste ancora con entrypoint completi e logica propria.

### Evidenze

- `WiredManager.java:136`
  - warning esplicito: `wired.engine.enabled / wired.engine.exclusive are now compatibility-only flags`
- `WiredManager.java:174`
  - `isEnabled()` dipende solo dall’inizializzazione del manager
- `WiredManager.java:182`
  - `isExclusive()` ritorna sempre `true`
- `WiredHandler.java:63`
  - entrypoint legacy completo `handle(...)`
- `WiredHandler.java:114`
  - supporto separato per `handleCustomTrigger(...)`

### Impatto pratico

Se qualunque pezzo di codice, plugin o path legacy entra ancora in `WiredHandler`, si possono avere:

- ordine effect diverso
- scheduling delay diverso
- condition flow diverso
- diagnostica/monitor non coerente col nuovo engine

### Fix suggerito

- definire un solo entrypoint runtime ufficiale
- se `WiredHandler` deve restare, trasformarlo in adapter minimo che inoltra sempre al nuovo engine
- aggiungere log o metriche per rilevare qualsiasi ingresso nel path legacy

---

## 3.3 Ordine degli effect non sempre deterministico senza `wf_xtra_exec_in_order`

- **Gravità:** Alta
- **Confidenza:** Alta
- **Area:** esecuzione stack

### Problema

Nel path legacy, l’ordinamento stabile viene applicato chiaramente solo in presenza di `wf_xtra_exec_in_order` oppure in casi specifici (`unseen`).

Negli altri casi, l’ordine si appoggia alla collezione che arriva dal runtime.

### Evidenze

- `WiredHandler.java:224`
  - rileva `hasExtraExecuteInOrder`
- `WiredHandler.java:230`
  - ordina con `WiredExecutionOrderUtil.sort(effects)` solo in alcuni casi
- `WiredHandler.java:249`
  - usa direttamente `effectList` in ordered mode

### Impatto pratico

Stack come:

- `move_rotate` + `match_to_sshot`
- `toggle` + `reset`
- `give_var` + `change_var_val`

possono produrre risultati diversi se si assume implicitamente un ordine che il runtime non promette davvero.

### Fix suggerito

- decidere se l’ordine stack deve essere sempre stabile di default
- in alternativa, mantenere la regola attuale ma documentarla in modo molto esplicito
- se si lascia la regola attuale, conviene segnalare in UI che l’ordine è garantito solo con `wf_xtra_exec_in_order`

---

## 3.4 Il path movimento legacy può ancora far vedere movimenti intermedi

- **Gravità:** Alta
- **Confidenza:** Alta
- **Area:** movement pipeline

### Problema

Il helper legacy di movimento usa ancora un fallback che, se il collector non è attivo, invia subito `FloorItemOnRollerComposer`.

Questo può far trapelare al client uno stato intermedio che in teoria avrebbe dovuto essere nascosto da batching o restore finale.

### Evidenze

- `WiredMoveCarryHelper.java:163`
  - metodo `moveFurniLegacy(...)`
- `WiredMoveCarryHelper.java:179`
  - usa il collector se disponibile
- `WiredMoveCarryHelper.java:196`
  - fallback diretto a `FloorItemOnRollerComposer`

### Impatto pratico

È coerente con il tipo di bug già visto:

- oggetto che “si vede muovere”
- poi viene riportato nello stato corretto
- ma il client ha già ricevuto un update intermedio

### Fix suggerito

- evitare qualsiasi composer diretto nel path legacy quando la logica wired moderna è attiva
- centralizzare tutti i movement update in un unico collector finale
- aggiungere test specifici per:
  - `move_rotate` + `match_to_sshot`
  - stacked move effects nello stesso tick

---

## 3.5 Tick a `50ms`, ma delay wired ancora a step da `500ms`

- **Gravità:** Media
- **Confidenza:** Alta
- **Area:** semantica temporale

### Problema

Il sistema oggi ha due granularità temporali diverse:

- repeaters / tickables a `50ms`
- delay wired classico a `delay * 500ms`

### Evidenze

- `WiredTickService.java:48`
  - `DEFAULT_TICK_INTERVAL_MS = 50`
- `WiredTickService.java:175`
  - `scheduleAtFixedRate(...)`
- `WiredEngine.java:753`
  - `long delayMs = delay * 500L`
- `WiredHandler.java:369`
  - stesso schema `delay * 500L`

### Impatto pratico

Non è per forza un bug, ma può creare:

- aspettative sbagliate nel builder dei wired
- sensazione di desync tra repeater e delay
- stack “velocissimi” su tick ma “grossolani” sugli effect ritardati

### Fix suggerito

- o si accetta questa doppia semantica e la si documenta ovunque
- o si introduce una nuova famiglia di delay high-resolution separata dal delay classico

---

## 3.6 `:wired` realtime a `50ms` può diventare rumoroso/pesante

- **Gravità:** Media
- **Confidenza:** Alta
- **Area:** tooling monitor/inspection

### Problema

Le request di monitor e variabili ora sono rate-limitate a `50ms`.

### Evidenze

- `WiredMonitorRequestEvent.java:39`
  - `return 50`
- `WiredUserVariablesRequestEvent.java:20`
  - `return 50`

### Impatto pratico

Su una stanza attiva o con più client staff aperti:

- carico rete maggiore
- più rumore sul server
- rischio di mascherare problemi reali con spam di refresh

### Fix suggerito

- spostare dove possibile a push/event driven
- lasciare `50ms` solo per il minimo indispensabile
- differenziare:
  - monitor heavy/debug
  - inspection live
  - variables snapshot

---

## 3.7 `click furni` ora è immediato: queue/cancel svuotati

- **Gravità:** Media
- **Confidenza:** Alta
- **Area:** eventi click furni

### Problema

La queue dei click furni è stata semplificata: ora il click parte subito, e il cancel path è vuoto.

### Evidenze

- `WiredManager.java:274`
  - `queueUserClicksFurni(...)` chiama subito `triggerUserClicksFurni(...)`
- `WiredManager.java:282`
  - `cancelPendingUserClicksFurni(...)` non fa nulla

### Impatto pratico

Se qualche comportamento vecchio dipendeva da:

- debounce
- cancel
- click differito

ora può cambiare senza che il mapping sia ovvio.

### Fix suggerito

- decidere se il comportamento immediato è quello definitivo
- se sì, documentarlo come breaking behavior
- se no, reintrodurre una queue reale con semantica esplicita

---

## 3.8 Semantica timestamp variabili non uniforme tra target type

- **Gravità:** Media
- **Confidenza:** Media
- **Area:** sistema variabili

### Problema

Le variabili utente e furni hanno senso come “assegnazione con creation/update time”, mentre le room/global variables hanno soprattutto senso sul solo `update time`.

Questo può diventare ambiguo quando si usano:

- `wf_cnd_var_age_match`
- sorting per creation/update
- UI manage/inspection

### Evidenze

- `WiredConditionVariableAgeMatch.java`
  - il target room/global vive soprattutto come valore di update
- le scelte di prodotto già fatte in `:wired` vanno in questa direzione

### Impatto pratico

- il builder può pensare che “tempo di creazione” sulle global sia forte quanto sulle user/furni
- condition o sort possono essere semanticamente strani anche se “funzionano”

### Fix suggerito

- trattare esplicitamente `room/global` come `updated-only`
- disabilitare in UI le opzioni che non hanno senso forte
- o documentare in modo molto chiaro la differenza

---

## 4. Backlog Consigliato

Ordine suggerito di intervento:

1. **Chiudere il target `context`**
   - o implementarlo davvero
   - o toglierlo da UI/save/runtime
2. **Unificare il runtime**
   - lasciare un solo entrypoint ufficiale
3. **Stabilire la regola sull’ordine effect**
   - default stabile o ordine esplicito con extra
4. **Chiudere il leak dei movement update legacy**
   - niente composer fuori collector quando wired moderno è attivo
5. **Ripensare il realtime di `:wired`**
   - spostare il più possibile da polling a push

---

## 5. Nota Finale

Il sistema wired attuale è già molto più potente del modello classico, soprattutto per:

- variabili
- signal routing
- selectors avanzati
- monitor
- manage/inspection

Proprio per questo, le zone fragili oggi non sono tanto i box semplici, ma:

- la coesistenza di due runtime
- la semantica temporale
- i movement stack
- le feature variabili ancora “mezze esposte”

Questi sono i punti che più probabilmente spiegano i bug strani o intermittenti.
