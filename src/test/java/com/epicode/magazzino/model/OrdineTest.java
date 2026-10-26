package com.epicode.magazzino.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdineTest {

    private static final LocalDateTime ADESSO = LocalDateTime.of(2026, 10, 26, 9, 0);

    private Prodotto viti;
    private Prodotto trapano;

    @BeforeEach
    void catalogo() {
        viti = new Prodotto("VIT", "Viti", new BigDecimal("6.90"), 100, 10);
        trapano = new Prodotto("TRA", "Trapano", new BigDecimal("89.00"), 5, 1);
    }

    @Test
    void ilTotaleESommaDeiSubtotali() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 3); // 20,70
        ordine.aggiungiRiga(trapano, 2); // 178,00
        assertEquals(new BigDecimal("198.70"), ordine.totale());
    }

    @Test
    void ogniRigaPrelevaDalMagazzino() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(trapano, 2);
        assertEquals(3, trapano.getDisponibili());
    }

    @Test
    void loStessoProdottoDueVolteFiniscenellaStessaRiga() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 3);
        ordine.aggiungiRiga(viti, 2);
        assertEquals(1, ordine.getRighe().size());
        assertEquals(5, ordine.getRighe().get(0).getQuantita());
    }

    @Test
    void ilPrezzoDellaRigaRestaQuelloDelMomentoDellOrdine() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 1);
        assertEquals(new BigDecimal("6.90"), ordine.getRighe().get(0).getPrezzoUnitario());
    }

    @Test
    void annullareRestituisceIPezzi() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(trapano, 2);
        ordine.annulla();
        assertEquals(StatoOrdine.ANNULLATO, ordine.getStato());
        assertEquals(5, trapano.getDisponibili());
    }

    @Test
    void unOrdineSpeditoNonSiAnnulla() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(trapano, 1);
        ordine.spedisci();
        assertThrows(OperazioneNonConsentitaException.class, ordine::annulla);
        assertEquals(4, trapano.getDisponibili());
    }

    @Test
    void unOrdineAnnullatoNonSiAnnullaDueVolte() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(trapano, 1);
        ordine.annulla();
        assertThrows(OperazioneNonConsentitaException.class, ordine::annulla);
        assertEquals(5, trapano.getDisponibili());
    }

    @Test
    void nonSiAggiungonoRigheAUnOrdineSpedito() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 1);
        ordine.spedisci();
        assertThrows(OperazioneNonConsentitaException.class, () -> ordine.aggiungiRiga(viti, 1));
    }

    @Test
    void clienteObbligatorio() {
        assertThrows(RichiestaNonValidaException.class, () -> new Ordine("  ", ADESSO));
    }

    @Test
    void gliScontiRispettanoLeSoglieDiQuantita() {
        // Ogni caso usa un prodotto indipendente per coprire i confini delle fasce.
        int[] quantita = { 1, 9, 10, 49, 50, 100, 101 };
        int[] percentualiAttese = { 0, 0, 5, 5, 10, 10, 10 };
        String[] subtotaliAttesi = { "2.00", "18.00", "19.00", "93.10", "90.00", "180.00", "181.80" };
        for (int indice = 0; indice < quantita.length; indice++) {
            Prodotto prodotto = new Prodotto("SCONTO", "Prodotto scontabile", new BigDecimal("2.00"), 101, 0);
            Ordine ordine = new Ordine("Rossi", ADESSO);
            ordine.aggiungiRiga(prodotto, quantita[indice]);
            assertEquals(percentualiAttese[indice], ordine.getRighe().get(0).getPercentualeSconto(),
                    "Quantita: " + quantita[indice]);
            assertEquals(new BigDecimal(subtotaliAttesi[indice]), ordine.getRighe().get(0).subtotale(),
                    "Quantita: " + quantita[indice]);
        }
    }

    @Test
    void loScontoSiArrotondaSoloSulSubtotaleFinale() {
        // Il netto 9,595 deve diventare 9,60, senza arrotondare prima lo sconto.
        Prodotto prodotto = new Prodotto("CENT", "Prodotto al centesimo", new BigDecimal("1.01"), 10, 0);
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(prodotto, 10);
        assertEquals(new BigDecimal("9.60"), ordine.getRighe().get(0).subtotale());
    }

    @Test
    void leAggiunteDuplicateRicalcolanoLoScontoSullaQuantitaTotale() {
        // Il passaggio 5+5 e poi 49+1 attraversa entrambe le soglie sulla stessa riga.
        Prodotto prodotto = new Prodotto("DUP", "Prodotto ripetuto", new BigDecimal("2.00"), 100, 0);
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(prodotto, 5);
        ordine.aggiungiRiga(prodotto, 5);
        assertEquals(1, ordine.getRighe().size());
        assertEquals(new BigDecimal("19.00"), ordine.totale());
        ordine.aggiungiRiga(prodotto, 39);
        assertEquals(new BigDecimal("93.10"), ordine.totale());
        ordine.aggiungiRiga(prodotto, 1);
        assertEquals(1, ordine.getRighe().size());
        assertEquals(new BigDecimal("90.00"), ordine.totale());
        assertEquals(50, prodotto.getDisponibili());
    }

    @Test
    void ilTotaleSommaISubtotaliScontatiGiaArrotondati() {
        // Due righe distinte provano che il totale somma i netti arrotondati per riga.
        Prodotto primo = new Prodotto("PRIMO", "Primo prodotto", new BigDecimal("2.00"), 10, 0);
        Prodotto secondo = new Prodotto("SECONDO", "Secondo prodotto", new BigDecimal("1.01"), 10, 0);
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(primo, 10);
        ordine.aggiungiRiga(secondo, 10);
        assertEquals(new BigDecimal("28.60"), ordine.totale());
    }

    @Test
    void laSpedizioneRispettaLaSogliaDiCentocinquantaEuroInclusa() {
        // Un solo pezzo isola i centesimi della soglia dalle fasce di sconto.
        String[] importiMerce = { "149.99", "150.00", "150.01" };
        String[] costiSpedizione = { "9.90", "0.00", "0.00" };
        String[] totaliDaPagare = { "159.89", "150.00", "150.01" };
        for (int indice = 0; indice < importiMerce.length; indice++) {
            Prodotto prodotto = new Prodotto("SOGLIA", "Prodotto alla soglia",
                    new BigDecimal(importiMerce[indice]), 1, 0);
            Ordine ordine = new Ordine("Rossi", ADESSO);
            ordine.aggiungiRiga(prodotto, 1);
            assertEquals(new BigDecimal(importiMerce[indice]), ordine.totale());
            assertEquals(new BigDecimal(costiSpedizione[indice]), ordine.costoSpedizione());
            assertEquals(new BigDecimal(totaliDaPagare[indice]), ordine.totaleDaPagare());
        }
    }

    @Test
    void laSogliaDiSpedizioneSiValutaDopoLoSconto() {
        // Il lordo supera 150 euro, ma il netto scontato non da' diritto alla
        // gratuita'.
        Prodotto prodotto = new Prodotto("NETTO", "Prodotto sotto soglia", new BigDecimal("15.50"), 10, 0);
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(prodotto, 10);
        assertEquals(new BigDecimal("147.25"), ordine.totale());
        assertEquals(new BigDecimal("9.90"), ordine.costoSpedizione());
        assertEquals(new BigDecimal("157.15"), ordine.totaleDaPagare());
    }

    @Test
    void loScontoNonModificaIPezziRestituitiAllAnnullamento() {
        // Lo sconto riguarda gli importi: tutti i pezzi prelevati devono essere
        // restituiti.
        Prodotto prodotto = new Prodotto("RESO", "Prodotto da restituire", new BigDecimal("2.00"), 100, 0);
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(prodotto, 50);
        assertEquals(50, prodotto.getDisponibili());
        ordine.annulla();
        assertEquals(100, prodotto.getDisponibili());
        assertEquals(new BigDecimal("90.00"), ordine.totale());
    }
}
