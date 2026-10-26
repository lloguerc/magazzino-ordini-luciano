# Project Overview

Gestionale didattico di magazzino e ordini: catalogo prodotti, disponibilita', scorta minima, creazione ordini, spedizione e annullamento con ripristino delle giacenze. L'interfaccia attuale e' composta da pagine HTML server-side, non da API REST.

Le indicazioni sul comportamento attuale derivano dal codice. Le prescrizioni etichettate **Regola futura** definiscono convenzioni per aspetti non ancora consolidati; non autorizzano a implementarli senza una richiesta.

# Technology Stack

- Java 25; Maven con Wrapper e `spring-boot-maven-plugin`.
- Spring Boot 4.1.1, dichiarato come parent nel [pom.xml](../pom.xml).
- `spring-boot-starter-webmvc`: Spring MVC.
- `spring-boot-starter-thymeleaf`: template HTML lato server.
- `spring-boot-starter-data-jpa`: Spring Data JPA, Jakarta Persistence e Hibernate.
- H2, dipendenza runtime, configurato in memoria.
- Test: `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`, JUnit Jupiter, Spring Boot Test, MockMvc e matcher Hamcrest.
- Non sono presenti API REST, documentazione OpenAPI/Swagger, uso esplicito di Bean Validation, Lombok o Spring Security. Non introdurli implicitamente.
- Lasciare al parent Maven la gestione delle versioni delle dipendenze, salvo necessita' motivata.

# Project Architecture

- Rispettare il flusso `controller -> service -> model/repository` descritto nel [README](../README.md).
- **Controller**: binding HTTP, invocazione dei service, attributi del model, viste, redirect e messaggi. Niente accesso diretto ai repository o regole di business.
- **Service**: casi d'uso, ricerca delle risorse e orchestrazione delle operazioni di dominio entro una transazione.
- **Repository**: accesso ai dati tramite interfacce `JpaRepository` e query derivate; niente regole di dominio.
- **Entity/Model**: entita' JPA con comportamento, invarianti, calcoli monetari e transizioni di stato. Non spostare queste responsabilita' nei controller o nei template.
- **DTO/Form**: `NuovoOrdine` e il record annidato `Riga` rappresentano la richiesta del caso d'uso; `OrdineForm` e `RigaForm` gestiscono il binding web.
- **Mapper**: non esiste un layer dedicato; la conversione attuale e' `OrdineForm.toRichiesta()`. Non aggiungere package DTO/mapper, interfacce service o framework di mapping senza una reale necessita'.
- Usare dependency injection tramite costruttore, mantenendo `final` le dipendenze, come nei service e controller esistenti.

# Package Structure

Collocare le nuove classi sotto `src/main/java/com/epicode/magazzino/` in base alla responsabilita':

| Package | Contenuto |
| --- | --- |
| radice | `MagazzinoApplication`, bootstrap Spring Boot |
| `config` | configurazione Spring, `OrologioConfig`, `DatiIniziali` |
| `model` | `Prodotto`, `Ordine`, `RigaOrdine`, `StatoOrdine`, eccezioni di dominio |
| `repository` | `ProdottoRepository`, `OrdineRepository` e nuove interfacce di persistenza pertinenti |
| `service` | `CatalogoService`, `OrdineService`, richieste dei casi d'uso come `NuovoOrdine` |
| `web` | controller MVC, form e `GestoreErrori` |

- Template in `src/main/resources/templates/`; riutilizzare `fragments.html` e lo stile in `src/main/resources/static/css/app.css`.
- Configurazione in [application.properties](../src/main/resources/application.properties).
- Test sotto `src/test/java/com/epicode/magazzino/`, nel package corrispondente al livello verificato.

# Java Coding Guidelines

- Conservare il lessico italiano del dominio e nomi descrittivi: classi `PascalCase`, metodi/variabili `camelCase`, costanti `UPPER_SNAKE_CASE`, package minuscoli. Mantenere i nomi tecnici delle API dei framework.
- Usare campi privati e la visibilita' minima necessaria; rendere pubbliche solo le operazioni richieste dai chiamanti. Preservare i costruttori senza argomenti `protected` delle entita' JPA e le operazioni interne package-private.
- Non introdurre setter generici per stato, righe o giacenze: usare metodi di dominio. I setter dei form restano necessari al binding.
- Usare `final` per dipendenze e valori non riassegnabili quando migliora la chiarezza; non rendere indiscriminatamente final classi o campi delle entita' JPA.
- Validare esplicitamente gli input obbligatori al confine pertinente, mantenendo le invarianti nel dominio. Non usare `null` come risultato ordinario di una ricerca o come collezione vuota.
- Rispettare gli `Optional` restituiti dai repository; usare `orElseThrow` con l'eccezione specifica, non `get()` non verificato. **Regola futura**: non usare `Optional` come campo JPA o parametro di input.
- Usare collezioni parametrizzate. Non esporre collezioni di dominio modificabili: `Ordine.getRighe()` restituisce una vista non modificabile, non una copia profondamente immutabile.
- Preferire cicli espliciti per mutazioni e flussi di dominio. **Regola futura**: usare Stream API solo se piu' leggibile, senza effetti collaterali o elaborazioni parallele sulle entita'.
- Riutilizzare record per richieste e valori appropriati, enum per insiemi chiusi come gli stati; non convertire in record entita' JPA o form mutabili.
- **Regola futura**: rendere immutabili i nuovi valori/DTO quando possibile, copiando le collezioni se serve un vero isolamento; un record non garantisce immutabilita' profonda.
- Usare `BigDecimal` per importi, mai `double`; preservare l'arrotondamento a due decimali `HALF_UP` dei subtotali. Non introdurre arrotondamenti intermedi che cambino i risultati.
- Usare il `Clock` iniettato per il tempo dei casi d'uso; la configurazione attuale usa `Clock.systemDefaultZone()`, non impone UTC.
- Preferire codice semplice, responsabilita' circoscritte e metodi leggibili. **Regola futura**: non abilitare funzionalita' preview Java senza accordo esplicito.

# Domain Guidelines

- Riutilizzare `Prodotto`, `Ordine`, `RigaOrdine` e `StatoOrdine`; non duplicarli con `Product`, `Inventory`, `Stock`, `Order` o `OrderItem`.
- `Prodotto` possiede codice unico, nome, prezzo, `disponibili` e `scortaMinima`. Prezzo, disponibilita' e scorta minima non possono essere negativi; codice e nome sono obbligatori.
- `preleva` e `riassortisci` richiedono quantita' strettamente positive. Un prelievo superiore alla disponibilita' deve fallire prima di modificare la giacenza.
- Un ordine nasce `CONFERMATO`, con cliente obbligatorio. Il caso d'uso di creazione richiede almeno una riga.
- `Ordine.aggiungiRiga` e' consentito solo in stato `CONFERMATO` e preleva subito dal prodotto. Lo stesso codice prodotto viene consolidato nella stessa riga.
- Non scalare nuovamente le quantita' alla spedizione; non esiste un meccanismo separato di prenotazione.
- `RigaOrdine` conserva il prezzo unitario acquisito alla creazione della riga; non ricalcolare lo storico dal prezzo corrente del catalogo.
- `RigaOrdine.getPercentualeSconto()` deriva lo sconto dalla quantita' consolidata: 0% da 1 a 9 pezzi, 5% da 10 a 49, 10% da 50 in su. Non persistere lo sconto o fissarlo prima del consolidamento.
- `RigaOrdine.subtotale()` applica lo sconto al prezzo acquisito per quantita' e arrotonda una sola volta alla fine, a due decimali `HALF_UP`. Non arrotondare separatamente lo sconto.
- `Ordine.totale()` somma i subtotali scontati e resta il totale della sola merce. `costoSpedizione()` restituisce 9,90 euro sotto 150,00 euro di merce gia' scontata, altrimenti 0,00. `totaleDaPagare()` somma merce e spedizione.
- Da `CONFERMATO` si puo' passare a `SPEDITO` oppure `ANNULLATO`. La spedizione non si ripete; un ordine spedito non si annulla. L'annullamento restituisce i pezzi una sola volta e non si ripete.
- Creazione e variazioni di stock del caso d'uso devono restare nella stessa transazione: nessun ordine parzialmente persistito se una riga fallisce.
- Sconti e spedizione dell'[esercizio M6-G1](../docs/corso/M6-G1.md) sono implementati. Il dettaglio mostra la percentuale per riga e i tre totali; l'elenco mostra il totale da pagare. Il [piano](../docs/piani/sconti-quantita.md) riporta le verifiche.

# REST API Guidelines

- Attualmente esistono controller MVC con Thymeleaf, non endpoint JSON. Conservare questo contratto finche' non viene richiesta un'API.
- Route attuali: `GET /` redirige a `/prodotti`; `GET /prodotti`, `/ordini`, `/ordini/nuovo`, `/ordini/{id}`; `POST /ordini`, `/ordini/{id}/spedisci`, `/ordini/{id}/annulla`.
- Usare GET per letture e POST per le azioni dei form. Preservare Post-Redirect-Get su successo, ritorno al form con errore in creazione e flash attribute nelle azioni sull'ordine.
- `OrdineForm.toRichiesta()` ignora righe con codice nullo/vuoto oppure quantita' nulla. Non cambiare silenziosamente questa semantica; la validazione server resta necessaria.
- Le entita' sono oggi usate come attributi delle viste: non confondere questo utilizzo con la loro esposizione come risposta REST.
- **Regole future, solo se viene richiesta un'API REST**: usare endpoint per risorse con naming coerente; GET legge, POST crea/esegue comandi, PUT sostituisce, PATCH modifica parzialmente, DELETE elimina quando il dominio lo consente.
- **Regole future REST**: definire DTO request/response espliciti, senza serializzare direttamente entita' JPA; validare input e invarianti; usare 200 per risposte con contenuto, 201 con `Location` per creazioni, 204 senza contenuto, 400 per input invalido, 404 per risorsa assente e 409 per conflitti di stato/scorta.
- **Regole future REST**: centralizzare risposte di errore uniformi con codice e messaggio sicuro, senza stack trace; non alterare per questo gli status e i messaggi dei form esistenti. Aggiungere OpenAPI solo se richiesto o giustificato.

# Persistence Guidelines

- Usare Spring Data JPA e query derivate come `findByCodice`, `findAllByOrderByNomeAsc` e `findAllByOrderByCreatoIlDesc`. Preservare ordinamenti e contratto `Optional` delle ricerche.
- Tenere le transazioni nei service; usare `@Transactional(readOnly = true)` per le letture. Mutare entita' gestite nella transazione, sfruttando il dirty checking senza salvataggi ridondanti.
- Preservare identificativi `Long` generati, vincolo univoco del codice prodotto, precisione/scala monetaria e stato persistito con `EnumType.STRING`.
- `Ordine` possiede le righe con `mappedBy`, `cascade = ALL` e `orphanRemoval = true`; `RigaOrdine` riferisce ordine e prodotto. Non propagare cancellazioni al catalogo prodotti o copiare queste cascade indiscriminatamente.
- `Ordine.righe` usa attualmente `EAGER`; non cambiarlo senza verificare letture e rendering. EAGER non garantisce l'assenza di N+1; **regola futura**: scegliere fetch/query mirati per nuove relazioni e verificarne gli effetti.
- Mantenere `spring.jpa.open-in-view=false`: preparare i dati necessari entro il confine transazionale, senza affidarsi a caricamenti lazy nei template.
- Configurazione attuale: H2 `jdbc:h2:mem:magazzino;DB_CLOSE_DELAY=-1`, Hibernate `ddl-auto=create-drop`, catalogo demo caricato da `DatiIniziali`. Il database e' volatile; non considerarlo persistenza produttiva.
- Non esistono migrazioni di schema configurate. **Regola futura**: per un database persistente concordare configurazione e migrazioni; non estendere `create-drop` a dati reali.
- Non sono presenti `@Version` o lock espliciti. Non attribuire a `@Transactional` protezione automatica da aggiornamenti persi o vendite oltre disponibilita'.
- **Regola futura**: quando un requisito coinvolge modifiche concorrenti alle giacenze, scegliere esplicitamente locking o aggiornamento atomico adeguato e aggiungere test concorrenti. Non introdurre tale modifica di nascosto in feature non correlate.

# Error Handling

- Riutilizzare la gerarchia unchecked `MagazzinoException`: `RichiestaNonValidaException` per input invalido, `RisorsaNonTrovataException` per risorse assenti, `OperazioneNonConsentitaException` per transizioni vietate, `ScortaInsufficienteException` per quantita' non disponibili.
- La validazione attuale e' imperativa nei costruttori/metodi di dominio e nei service; non presumere l'esistenza di `@Valid` o di uno starter Bean Validation.
- Conservare messaggi di dominio comprensibili in italiano e privi di dettagli tecnici. I vincoli JPA non sostituiscono la validazione del dominio.
- `GestoreErrori` usa `@ControllerAdvice` per restituire vista `errore` e HTTP 404 per `RisorsaNonTrovataException` non intercettate localmente.
- Preservare i catch locali di `MagazzinoException` per errori del form e messaggi flash; non trasformare tutti gli errori MVC in JSON o in 404.
- Lasciare propagare gli errori dal service per consentire il rollback; non catturare e ignorare errori dopo modifiche parziali.
- **Regola futura**: gestire gli errori tecnici inattesi con messaggio generico e status 500, registrando il dettaglio una sola volta; non mostrarlo all'utente o convertirlo falsamente in errore di validazione.

# Logging

Non esiste una strategia di logging applicativo esplicita nel codice attuale. **Regole future**:

- Quando necessario, usare SLF4J con il logging fornito dallo stack Spring Boot, senza aggiungere un secondo framework.
- Registrare a INFO solo eventi operativi significativi, come creazione e transizioni di un ordine, con identificativi tecnici; usare DEBUG per diagnostica e ERROR per guasti inattesi.
- Registrare l'errore tecnico con stack trace una sola volta al confine che lo gestisce. Gli input errati e le normali violazioni di dominio non richiedono stack trace o log ripetuti.
- Non usare `System.out.println` o `printStackTrace`; non registrare secret, dati personali del cliente, payload completi o oggetti entita' indiscriminatamente.

# Testing Guidelines

- Ogni nuova feature o correzione deve includere test pertinenti al livello modificato, riutilizzando suite e fixture esistenti.
- Dominio: test JUnit Jupiter senza Spring, come `ProdottoTest` e `OrdineTest`; verificare risultati, eccezioni e assenza di mutazioni quando l'operazione viene rifiutata.
- Service: seguire `OrdineServiceTest`, con `@SpringBootTest`, `@Transactional` e repository reali su H2 per verificare persistenza e casi d'uso.
- Web: seguire `PagineTest`, con `@SpringBootTest`, `@AutoConfigureMockMvc` e MockMvc; verificare status, contenuto, binding, redirect ed errori. Usare gli import compatibili con Spring Boot 4 gia' presenti.
- Non esistono test repository dedicati. **Regola futura**: aggiungerli per nuove query, vincoli o mapping non adeguatamente verificati dai test di integrazione esistenti.
- Mockito non e' usato nei test attuali. **Regola futura**: usarlo solo quando serve isolare dipendenze; verificare interazioni significative e non sostituire con mock le prove di transazioni, query o consistenza dello stock.
- Coprire, dove pertinenti: happy path, input non valido, quantita' zero/negative, disponibilita' esatta e insufficiente, risorsa assente, transizione vietata, righe duplicate e comportamento delle dipendenze rilevanti.
- **Regola futura**: per modifiche all'atomicita', verificare il rollback di un ordine multiriga rileggendo il database in una nuova transazione; il rollback non ripristina gli oggetti Java gia' mutati. Per requisiti concorrenti aggiungere prove dedicate.
- Usare nomi descrittivi in italiano e fixture indipendenti dall'ordine di esecuzione. **Regola futura**: isolare i dati dei nuovi test che mutano lo stock, senza dipendere da modifiche al catalogo demo effettuate da altri test; usare `Clock.fixed` per nuove regole temporali.
- Con JDK 25, eseguire dalla radice in PowerShell:

```powershell
.\mvnw.cmd "-Dtest=ProdottoTest,OrdineTest" test
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

Il primo comando e' un esempio di controllo mirato: scegliere i test della modifica. Eseguire la suite completa prima di dichiarare una feature completa; riportare i comandi realmente eseguiti e gli eventuali impedimenti.

# Feature Implementation Workflow

1. Leggere codice, test e documentazione pertinenti alla feature.
2. Individuare classi, metodi e componenti riutilizzabili.
3. Evitare duplicazioni di codice e concetti di dominio.
4. Mantenere architettura, package e convenzioni esistenti.
5. Modificare il minor numero di componenti necessario.
6. Implementare invarianti nel model e orchestrazione/transazioni nei service.
7. Aggiungere o aggiornare test e avviare prima i controlli mirati.
8. Verificare validazione, eccezioni e rollback pertinenti.
9. Verificare impatti su route, form, template ed eventuali API.
10. Verificare impatti su mapping, query, giacenze e configurazione del database.
11. Preservare la retrocompatibilita' quando possibile, eseguire i controlli finali e documentare cambiamenti necessari.

# Security Guidelines

- Non inserire password, token, API key, connection string con credenziali o altri secret nel codice, nei test, nei documenti o nei file versionati.
- **Regola futura**: fornire eventuali credenziali tramite configurazione esterna o variabili d'ambiente risolte da Spring; usare valori fittizi nei test e nessun secret nei log.
- Non esporre stack trace, dettagli SQL o configurazioni sensibili nelle pagine/error response. Validare sempre lato server, anche se il form contiene vincoli HTML.
- Non disabilitare l'escaping Thymeleaf per contenuti utente.
- Non e' configurato Spring Security; non presumere autenticazione, autorizzazione o protezione CSRF. **Regola futura**: concordare questi requisiti prima di un utilizzo produttivo, senza aggiungerli come refactoring collaterale.

# Copilot Behaviour

1. Leggere il codice correlato prima di modificarlo.
2. Rispettare architettura e naming esistenti.
3. Riutilizzare classi e metodi prima di crearne altri.
4. Evitare duplicazioni di comportamento e modello.
5. Non aggiungere dipendenze senza una reale necessita' motivata.
6. Non modificare API pubbliche, route o firme senza motivo legato alla richiesta.
7. Non mescolare refactoring o aggiornamenti tecnologici non richiesti alla feature.
8. Produrre codice semplice, leggibile e testabile.
9. Aggiungere test per il comportamento nuovo o corretto.
10. Non inventare requisiti funzionali; una specifica didattica non autorizza da sola l'implementazione.
11. Fare assunzioni minime ed esplicite; chiedere chiarimenti quando cambiano semantica di dominio o contratto.
12. Mantenere il comportamento esistente salvo richiesta esplicita.

- Preservare le modifiche dell'utente; non modificare manualmente `target/` o altri output generati.
- Rispettare [.gitignore](../.gitignore) e [.gitattributes](../.gitattributes): niente output di build, database locali o metadati IDE; Java e `mvnw` con LF, `mvnw.cmd` con CRLF.
- Non e' accertata una convenzione generale per commit o branch. Il branch suggerito nell'esercizio non e' una policy globale. **Regola futura**: non creare branch, commit o push senza richiesta; preferire modifiche focalizzate e descrizioni verificabili.
- Aggiornare queste istruzioni quando cambia lo stack o viene stabilita una nuova convenzione, distinguendo sempre fatti attuali e regole future.

# Definition of Done

- Il codice rispetta convenzioni, naming e responsabilita' del progetto.
- Compila con JDK 25 e Maven Wrapper.
- I test esistenti continuano a funzionare e sono stati aggiunti quelli necessari al cambiamento.
- Gli errori sono gestiti al livello appropriato, senza compromettere il rollback.
- Non sono state introdotte duplicazioni evitabili o dipendenze non necessarie.
- Route, form, template ed eventuali API sono coerenti con i contratti esistenti.
- Le modifiche alla persistenza rispettano il modello, i vincoli e la consistenza delle giacenze; la concorrenza e' verificata quando richiesta.
- Non sono presenti secret o configurazioni sensibili hard-coded.
- Documentazione pertinente aggiornata; comandi eseguiti, risultati e limiti di verifica comunicati senza dichiarare controlli non effettuati.