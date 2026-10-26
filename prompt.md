# Prompt Per I Test Delle Regole R1-R5

## R1: Fasce Di Sconto

Verifica i test esistenti per gli sconti di quantita' e completa quelli mancanti in `OrdineTest`, senza creare nuove classi o file. Con prezzo unitario 2,00 euro, controlla quantita' 1, 9, 10, 49, 50 e 100: le percentuali devono essere rispettivamente 0%, 0%, 5%, 5%, 10%, 10% e i subtotali 2,00 euro, 18,00 euro, 19,00 euro, 93,10 euro, 90,00 euro, 180,00 euro. Usa nomi e commenti in italiano. Esegui i test mirati e riporta l'esito, senza modificare la logica applicativa.

## R2: Arrotondamento

Verifica che `RigaOrdine.subtotale()` applichi lo sconto e arrotondi soltanto il risultato finale a due decimali con `HALF_UP`. Riutilizza `OrdineTest`: 10 pezzi da 1,01 euro con sconto 5% producono 9,595 euro, quindi 9,60 euro, non 9,59 euro. Usa `BigDecimal`, valori attesi espliciti e commenti in italiano. Non duplicare test gia' presenti, non creare file e non modificare il codice applicativo. Esegui il controllo mirato.

## R3: Quantita' Consolidata

Verifica in `OrdineTest` che due aggiunte dello stesso prodotto, 5 pezzi piu' 5 pezzi a 2,00 euro, producano una sola riga da 10 pezzi, sconto 5%, subtotale 19,00 euro e prelievo complessivo di 10 pezzi. Verifica anche il passaggio da 49 a 50 pezzi: lo sconto dell'intera riga deve diventare 10%. Completa solo i test mancanti, con nomi e commenti italiani, senza creare file o modificare la logica applicativa. Esegui i test mirati.

## R4: Totale Merce

Verifica in `OrdineTest` che `Ordine.totale()` sommi i subtotali gia' scontati e arrotondati, senza includere la spedizione. Usa due prodotti distinti: 10 pezzi da 2,00 euro producono 19,00 euro; 10 pezzi da 1,01 euro producono 9,60 euro. Il totale merce atteso e' 28,60 euro. Riutilizza i test esistenti senza duplicazioni, usa commenti italiani e non modificare il codice applicativo. Esegui il test e riporta l'esito.

## R5: Spedizione

Verifica in `OrdineTest` costo di spedizione e totale da pagare per questi totali merce: 149,99 euro -> spedizione 9,90 euro, totale 159,89 euro; 150,00 euro -> spedizione 0,00 euro, totale 150,00 euro; 150,01 euro -> spedizione 0,00 euro, totale 150,01 euro. Verifica inoltre che la soglia sia valutata dopo lo sconto: 10 pezzi da 15,50 euro producono merce 147,25 euro, spedizione 9,90 euro e totale 157,15 euro. Completa solo i test mancanti, con nomi e commenti italiani, senza nuovi file o modifiche alla logica applicativa. Esegui i test mirati e riporta i risultati.