# Piano: sconti per quantita' e spedizione

## Obiettivo

Applicare sconti per quantita' alle righe degli ordini e offrire la spedizione gratuita agli ordini con totale merce di almeno 150,00 euro. Aggiornare dettaglio ed elenco ordini, preservando le regole esistenti di giacenza, prezzo acquisito e ciclo di vita dell'ordine.

Stato: implementati dominio, persistenza e viste R1-R6; completate le attivita' T001-T006 e T008. Build pulita completata con 34 test superati. T007 e la verifica su runtime JDK 25 di T009 restano da eseguire; vedere il resoconto finale.

## Requisiti

| Regola | Comportamento richiesto |
| --- | --- |
| R1 | Sconto per riga: 0% da 1 a 9 pezzi, 5% da 10 a 49, 10% da 50 in su. |
| R2 | Subtotale = prezzo unitario per quantita', meno lo sconto, arrotondato al centesimo con `HALF_UP`. |
| R3 | La fascia dipende dalla quantita' complessiva della riga: due aggiunte dello stesso prodotto da 5 pezzi producono una riga da 10 pezzi con sconto 5%. |
| R4 | Il totale merce e' la somma dei subtotali scontati. |
| R5 | Spedizione di 9,90 euro sotto 150,00 euro di totale merce; gratuita da 150,00 euro inclusi. Totale da pagare = merce + spedizione. |
| R6 | Il dettaglio mostra la percentuale di sconto per riga e i tre importi finali; l'elenco mostra il totale da pagare. |

## Decisioni Tecniche

- Conservare il flusso `controller -> service -> model/repository`: i calcoli restano nel dominio, non nei controller o nei template.
- In [RigaOrdine](../../src/main/java/com/epicode/magazzino/model/RigaOrdine.java), aggiungere `getPercentualeSconto()`, calcolato dalla quantita' corrente, con risultato intero 0, 5 o 10. Non aggiungere campi persistiti.
- Modificare `subtotale()` usando il prezzo unitario acquisito e il fattore esatto `(100 - percentuale) / 100`. Usare solo `BigDecimal` e arrotondare una volta, sul risultato finale, a due decimali con `HALF_UP`.
- Non arrotondare separatamente l'importo dello sconto o il prezzo unitario scontato.
- Riutilizzare il consolidamento per codice prodotto gia' presente in `Ordine.aggiungiRiga()`: lo sconto si adegua automaticamente alla quantita' aggregata.
- In [Ordine](../../src/main/java/com/epicode/magazzino/model/Ordine.java), mantenere `totale()` come totale merce, senza spedizione. Non rinominarlo o aggiungere alias non necessari.
- Aggiungere `costoSpedizione()` e `totaleDaPagare()`, con risultati `BigDecimal` a due decimali. Definire soglia e tariffa come costanti private `static final`.
- Confrontare il totale merce gia' scontato con 150,00 tramite `compareTo`; restituire 9,90 sotto soglia, 0,00 altrimenti.
- Non introdurre tariffe speciali in base allo stato. Il service continua a rifiutare ordini senza righe.
- Nessuna nuova dipendenza, entita', migrazione di schema o modifica delle transazioni e delle regole di stock.

## Fase 1: Dominio E Controlli Mirati

- [ ] T001: estendere [OrdineTest](../../src/test/java/com/epicode/magazzino/model/OrdineTest.java) con test R1-R3 e R4, usando prodotti creati nelle fixture. Verificare prima i fallimenti attesi dei nuovi casi.
- [ ] T002: implementare percentuale e subtotale scontato in `RigaOrdine`; eseguire subito i test mirati disponibili. Dipende da T001.
- [ ] T003: aggiungere i test R5 e implementare `costoSpedizione()` e `totaleDaPagare()` in `Ordine`; eseguire `OrdineTest` e `ProdottoTest`. Dipende da T002.

Introdurre i test in passi eseguibili: non aggiungere contemporaneamente riferimenti a tutti i nuovi metodi se questo impedisce di verificare la singola modifica. Preservare i test esistenti su prelievi, disponibilita' insufficiente, prezzo acquisito, spedizione e annullamento.

## Fase 2: Persistenza E Pagine

- [ ] T004: estendere [OrdineServiceTest](../../src/test/java/com/epicode/magazzino/service/OrdineServiceTest.java) con un ordine contenente due richieste da 5 pezzi dello stesso prodotto. Verificare riga consolidata, sconto, importi e scorta; eseguire flush e clear del persistence context e rileggere l'ordine per provare il comportamento sui dati persistiti. Dipende da T003.
- [ ] T005: aggiornare [ordine-dettaglio.html](../../src/main/resources/templates/ordine-dettaglio.html) aggiungendo la quinta colonna "Sconto", percentuale con simbolo `%` e tre righe finali: "Totale merce", "Spedizione", "Totale da pagare". Adeguare i `colspan` a 4. Aggiornare [ordini.html](../../src/main/resources/templates/ordini.html) per usare `totaleDaPagare()` e l'intestazione "Totale da pagare". Dipende da T003.
- [ ] T006: estendere [PagineTest](../../src/test/java/com/epicode/magazzino/web/PagineTest.java) con prodotti dedicati e ordini creati dal service, isolando le mutazioni mediante transazioni di test. Verificare dettaglio ed elenco con MockMvc e asserzioni mirate alle celle, non solo alla presenza generica degli importi. Coprire spedizione pagata e gratuita. Eseguire i test web dopo le modifiche alle viste. Dipende da T005.
- [ ] T007: verificare il rendering su desktop e mobile, se gli strumenti disponibili lo consentono. Solo se necessario, introdurre un contenitore con scorrimento orizzontale per la tabella in [app.css](../../src/main/resources/static/css/app.css); nessun redesign. Dipende da T005 e T006.

Mantenere la formattazione italiana degli importi a due decimali e il simbolo euro. Non usare il catalogo demo come fixture mutabile condivisa. Non cambiare controller, service, repository o form se i metodi di dominio e i template soddisfano i requisiti.

Il test di integrazione esistente con 4 pezzi da 2,50 euro e un pezzo da 15,00 euro deve mantenere `totale()` pari a 25,00 euro; aggiungere le verifiche di spedizione 9,90 e totale da pagare 34,90 euro.

## Fase 3: Documentazione E Verifica Finale

- [ ] T008: aggiornare il [README](../../README.md) e le [istruzioni Copilot](../../.github/copilot-instructions.md) per descrivere sconti e spedizione come comportamento implementato, chiarendo la distinzione tra totale merce e totale da pagare. Non modificare la specifica dell'[esercizio](../corso/M6-G1.md). Dipende dal completamento delle fasi 1 e 2.
- [ ] T009: eseguire suite completa e `clean verify` con JDK 25 e Maven Wrapper; riportare comandi, risultati ed eventuali impedimenti. Verificare le modifiche senza includere refactoring estranei. Dipende da T004-T008.

Non creare branch, commit o push senza una richiesta esplicita. Se Git non e' disponibile nella cartella di lavoro, segnalarlo senza inizializzare un repository.

## Casi Di Verifica

### Fasce E Arrotondamento

| Prezzo unitario | Quantita' | Sconto | Subtotale atteso |
| --- | --- | --- | --- |
| 2,00 | 1 | 0% | 2,00 |
| 2,00 | 9 | 0% | 18,00 |
| 2,00 | 10 | 5% | 19,00 |
| 2,00 | 49 | 5% | 93,10 |
| 2,00 | 50 | 10% | 90,00 |
| 2,00 | 100 | 10% | 180,00 |
| 1,01 | 10 | 5% | 9,60 |

L'ultimo caso produce 9,595 prima dell'arrotondamento: il risultato deve essere 9,60, non 9,59 ottenuto arrotondando separatamente lo sconto.

- Aggregazione R3: 5 + 5 pezzi a 2,00 euro diventano una sola riga da 10, sconto 5%, subtotale 19,00 e prelievo complessivo di 10 pezzi. Verificare anche il passaggio 49 + 1 alla fascia 10%.
- Somma R4: una riga da 19,00 e una da 9,60 producono totale merce 28,60. Sommare gli importi gia' arrotondati per riga.
- Persistenza: due aggiunte da 5 pezzi di `T-VITI` a 2,50 euro producono una riga da 10, merce 23,75, spedizione 9,90 e totale da pagare 33,65. Con disponibilita' iniziale 20, la scorta residua e' 10.

### Soglia Di Spedizione

| Totale merce | Spedizione | Totale da pagare |
| --- | --- | --- |
| 149,99 | 9,90 | 159,89 |
| 150,00 | 0,00 | 150,00 |
| 150,01 | 0,00 | 150,01 |

Per isolare la soglia usare prodotti da un pezzo, senza sconto. Aggiungere una prova sulla soglia dopo lo sconto: 10 pezzi da 15,50 euro hanno lordo 155,00 ma merce scontata 147,25; la spedizione costa quindi 9,90 e il totale da pagare e' 157,15.

### Rendering E Regressioni

- Dettaglio con percentuali 0%, 5% e 10% corrette, cinque colonne, tre righe di riepilogo e importi coerenti con il dominio.
- Elenco con totale da pagare, non soltanto totale merce.
- Formattazione italiana, nessuna sovrapposizione di celle o importi nei viewport verificati.
- Prezzo acquisito, prelievi, rifiuto per scorta insufficiente, transizioni di stato e restituzione delle giacenze invariati.
- Test web esistenti di creazione, redirect, errori del form e 404 ancora validi.

## Comandi Di Verifica Previsti

Da PowerShell nella radice del progetto, con JDK 25:

```powershell
.\mvnw.cmd "-Dtest=ProdottoTest,OrdineTest" test
.\mvnw.cmd "-Dtest=OrdineServiceTest,PagineTest" test
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

Verifica aggiuntiva facoltativa suggerita dall'esercizio: modificare temporaneamente la soglia 10 in 11 e accertare che almeno un test fallisca; poi ripristinare esclusivamente la modifica introdotta per la prova.

## Tracciabilita' Dei Requisiti

| Requisito | Attivita' | Evidenza di completamento |
| --- | --- | --- |
| R1 | T001, T002, T006 | Percentuale calcolata; test delle fasce e visualizzazione. |
| R2 | T001, T002 | Subtotale scontato e test dell'arrotondamento finale. |
| R3 | T001, T004 | Aggregazione esistente verificata, anche dopo rilettura dal database. |
| R4 | T001, T003, T004 | `totale()` come somma dei subtotali scontati. |
| R5 | T003, T004 | Metodi di spedizione e totale da pagare; test dei limiti e soglia post-sconto. |
| R6 | T005, T006, T007 | Dettaglio ed elenco aggiornati, test MockMvc e verifica del rendering. |

## Criteri Di Completamento

- Tutte le regole R1-R6 coperte da implementazione e test pertinenti.
- Suite esistente verde e build verificata con i comandi effettivamente eseguiti.
- Nessun nuovo campo persistito, dipendenza o cambiamento non richiesto a route, stock, transazioni o schema.
- `totale()` conserva il significato di totale merce; le viste usano il totale da pagare dove richiesto.
- Documentazione aggiornata e limiti delle verifiche dichiarati.

## Resoconto Delle Verifiche

Il 26 ottobre 2026 sono stati eseguiti il Wrapper tramite percorso assoluto e l'opzione `-f` sul `pom.xml` del progetto, con questi risultati:

- `-Dtest=OrdineTest test`: prima 4 fallimenti attesi su 13 test; dopo il calcolo degli sconti, 13 test superati.
- `-Dtest=ProdottoTest,OrdineTest test`: 21 test superati dopo l'aggiunta della spedizione.
- `-Dtest=OrdineServiceTest test`: 6 test superati, incluse rilettura dal database e giacenze.
- `-Dtest=PagineTest test`, con i limiti JVM sotto indicati: prima 3 fallimenti attesi su 7 test; dopo l'aggiornamento dei template, 7 test superati.
- `"-DargLine=-Xmx256m -XX:ReservedCodeCacheSize=64m -XX:ActiveProcessorCount=2" clean verify`: BUILD SUCCESS, 34 test, nessun fallimento, errore o test saltato; JAR generato.

Ambiente effettivo: Java 26.0.2.1, compilazione con `release 25`. Nessun JDK 25 trovato nelle installazioni controllate: la verifica su quel runtime resta aperta.

Un primo avvio dei test web senza limiti JVM e' terminato per memoria nativa insufficiente; i parametri temporanei `argLine` hanno consentito la verifica senza modificare Maven o le dipendenze.

Il rendering dei template e' verificato da MockMvc. La verifica visuale desktop/mobile non e' stata eseguita: l'avvio del server e' stato saltato su indicazione dell'utente. Nessuna modifica CSS introdotta.

Non sono state create nuove classi per la feature; le nuove operazioni e i commenti sono in italiano.