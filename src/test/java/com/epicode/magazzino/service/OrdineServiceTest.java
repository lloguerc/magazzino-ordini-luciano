package com.epicode.magazzino.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epicode.magazzino.model.Ordine;
import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.RichiestaNonValidaException;
import com.epicode.magazzino.model.RisorsaNonTrovataException;
import com.epicode.magazzino.model.ScortaInsufficienteException;
import com.epicode.magazzino.repository.ProdottoRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Test di integrazione: service, repository e database H2 veri. */
@SpringBootTest
@Transactional
class OrdineServiceTest {

    @Autowired
    private OrdineService ordini;

    @Autowired
    private ProdottoRepository prodotti;

    // La rilettura fuori dalla cache JPA verifica i valori realmente persistiti.
    @Autowired
    private EntityManager entita;

    @BeforeEach
    void prodottiDiTest() {
        prodotti.save(new Prodotto("T-VITI", "Viti di test", new BigDecimal("2.50"), 20, 5));
        prodotti.save(new Prodotto("T-MART", "Martello di test", new BigDecimal("15.00"), 3, 1));
    }

    @Test
    void creaUnOrdineEScalaLeScorte() {
        Ordine ordine = ordini.crea(new NuovoOrdine("Ferramenta Bianchi", List.of(
                new NuovoOrdine.Riga("T-VITI", 4),
                new NuovoOrdine.Riga("T-MART", 1))));

        assertNotNull(ordine.getId());
        assertEquals(new BigDecimal("25.00"), ordine.totale());
        // Il totale merce resta separato dalla tariffa e dall'importo da pagare.
        assertEquals(new BigDecimal("9.90"), ordine.costoSpedizione());
        assertEquals(new BigDecimal("34.90"), ordine.totaleDaPagare());
        assertEquals(16, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());
        assertEquals(2, prodotti.findByCodice("T-MART").orElseThrow().getDisponibili());
    }

    @Test
    void prodottoInesistente() {
        NuovoOrdine richiesta = new NuovoOrdine("Bianchi", List.of(new NuovoOrdine.Riga("NON-ESISTE", 1)));
        assertThrows(RisorsaNonTrovataException.class, () -> ordini.crea(richiesta));
    }

    @Test
    void scortaInsufficiente() {
        NuovoOrdine richiesta = new NuovoOrdine("Bianchi", List.of(new NuovoOrdine.Riga("T-MART", 4)));
        assertThrows(ScortaInsufficienteException.class, () -> ordini.crea(richiesta));
    }

    @Test
    void ordineSenzaRighe() {
        NuovoOrdine richiesta = new NuovoOrdine("Bianchi", List.of());
        assertThrows(RichiestaNonValidaException.class, () -> ordini.crea(richiesta));
    }

    @Test
    void scontoETotaliDelleRigheConsolidateRestanoCorrettiDopoLaRilettura() {
        // Due richieste dello stesso prodotto devono persistere una sola riga scontata.
        Ordine ordine = ordini.crea(new NuovoOrdine("Cliente sconto", List.of(
                new NuovoOrdine.Riga("T-VITI", 5),
                new NuovoOrdine.Riga("T-VITI", 5))));
        Long identificativo = ordine.getId();
        entita.flush();
        entita.clear();

        // Il nuovo caricamento non riutilizza gli oggetti Java mutati nella creazione.
        Ordine riletto = ordini.trova(identificativo);
        assertEquals(1, riletto.getRighe().size());
        assertEquals(10, riletto.getRighe().get(0).getQuantita());
        assertEquals(5, riletto.getRighe().get(0).getPercentualeSconto());
        assertEquals(new BigDecimal("2.50"), riletto.getRighe().get(0).getPrezzoUnitario());
        assertEquals(new BigDecimal("23.75"), riletto.totale());
        assertEquals(new BigDecimal("9.90"), riletto.costoSpedizione());
        assertEquals(new BigDecimal("33.65"), riletto.totaleDaPagare());
        assertEquals(10, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());
    }

    @Test
    void laSpedizioneGratuitaSiRicalcolaDaiDatiPersistiti() {
        // Cinquanta pezzi a 3,34 euro restano sopra soglia dopo lo sconto del 10%.
        prodotti.save(new Prodotto("T-GRATIS", "Prodotto spedizione gratuita", new BigDecimal("3.34"), 50, 0));
        Ordine ordine = ordini.crea(new NuovoOrdine("Cliente spedizione", List.of(
                new NuovoOrdine.Riga("T-GRATIS", 50))));
        Long identificativo = ordine.getId();
        entita.flush();
        entita.clear();

        // Percentuale e tariffa sono derivate, senza colonne aggiuntive nel database.
        Ordine riletto = ordini.trova(identificativo);
        assertEquals(10, riletto.getRighe().get(0).getPercentualeSconto());
        assertEquals(new BigDecimal("150.30"), riletto.totale());
        assertEquals(new BigDecimal("0.00"), riletto.costoSpedizione());
        assertEquals(new BigDecimal("150.30"), riletto.totaleDaPagare());
        assertEquals(0, prodotti.findByCodice("T-GRATIS").orElseThrow().getDisponibili());
    }
}
