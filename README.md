# Magazzino & Ordini

L'applicazione del **Modulo 6** del corso *Agentic Software Development*: un piccolo
gestionale di ordini e magazzino su cui lavorano gli agenti di **GitHub Copilot**
(agent mode, plan mode, cloud agent, code review).

È volutamente semplice: il protagonista del modulo non è il codice ma come lo si fa
scrivere, verificare e rivedere agli agenti.

## Stack

| | |
|---|---|
| Linguaggio | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Data JPA) |
| Pagine | Thymeleaf (template HTML lato server) |
| Database | H2 in memoria: riparte dal catalogo iniziale a ogni avvio |
| Test | JUnit 5, Spring Boot Test, MockMvc |
| Build | Maven Wrapper (`mvnw`) |

## Avvio

```powershell
.\mvnw.cmd test              # tutti i test devono essere verdi
.\mvnw.cmd spring-boot:run   # poi apri http://localhost:8080
```

## Come è fatto

```
src/main/java/com/epicode/magazzino
├── model/        entità e regole di business (Prodotto, Ordine, RigaOrdine, MovimentoMagazzino, eccezioni)
├── repository/   Spring Data JPA
├── service/      casi d'uso transazionali (CatalogoService, OrdineService, MovimentiService)
├── web/          controller Thymeleaf, form, gestione errori
└── config/       orologio (Clock) e dati iniziali
src/main/resources/templates   le pagine Thymeleaf
src/test/java                  test di dominio (senza Spring) e di integrazione
```

Il flusso di una richiesta: **controller → service → model/repository**. Il controller
non contiene regole; le regole stanno nel model e i service le orchestrano in una
transazione.

## Regole di business già presenti

- Un ordine nasce **CONFERMATO**; può diventare **SPEDITO** oppure **ANNULLATO**, nient'altro.
- Ogni riga preleva i pezzi dal magazzino; se non bastano l'ordine non viene creato.
- Lo stesso prodotto inserito due volte finisce nella stessa riga.
- Il prezzo della riga è quello del prodotto al momento dell'ordine.
- Annullare restituisce i pezzi al magazzino; un ordine spedito non si annulla.
- Importi in `BigDecimal`, arrotondati al centesimo.
- Sconto sulla quantita' consolidata della riga: 0% da 1 a 9 pezzi, 5% da 10 a 49, 10% da 50 in su; arrotondamento `HALF_UP` solo sul subtotale finale.
- `Ordine.totale()` resta il totale merce scontato. Spedizione di 9,90 euro sotto 150,00 euro di merce, gratuita da 150,00 euro inclusi.
- `totaleDaPagare()` somma merce e spedizione. Il dettaglio mostra sconto e riepilogo; l'elenco mostra il totale da pagare.
- I riassortimenti registrano un movimento `CARICO`; la creazione e l'annullamento di un ordine registrano rispettivamente `SCARICO` e `RESO`. Lo storico è consultabile in `/movimenti`.

Piano e verifiche: [docs/piani/sconti-quantita.md](docs/piani/sconti-quantita.md).

Se la JVM di Surefire termina per memoria nativa insufficiente, usare limiti temporanei senza modificare il progetto:

```powershell
.\mvnw.cmd "-DargLine=-Xmx256m -XX:ReservedCodeCacheSize=64m -XX:ActiveProcessorCount=2" clean verify
```

## Usarlo come template (docente)

1. Pubblica il repo su GitHub e attiva **Settings → Template repository**.
2. Ogni studente: **Use this template → Create a new repository**, proprietario il
   proprio account, visibilità **Public** (serve per CodeQL e secret scanning gratuiti
   nella giornata 3), senza "Include all branches".
3. Il branch `soluzione/m6-g1` e i tag restano nel repo del docente: un repository
   creato dal template parte dal solo `main`.

Le istruzioni del repository sono in [.github/copilot-instructions.md](.github/copilot-instructions.md).

## Giornate

| Giornata | Consegna |
|---|---|
| M6 · G1 — Feature authoring e test generation | [docs/corso/M6-G1.md](docs/corso/M6-G1.md) |
