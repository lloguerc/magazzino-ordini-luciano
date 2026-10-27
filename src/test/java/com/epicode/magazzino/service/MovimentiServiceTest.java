package com.epicode.magazzino.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epicode.magazzino.model.MovimentoMagazzino;
import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.RichiestaNonValidaException;
import com.epicode.magazzino.model.TipoMovimento;
import com.epicode.magazzino.repository.MovimentoRepository;
import com.epicode.magazzino.repository.ProdottoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MovimentiServiceTest {

    @Autowired
    private CatalogoService catalogo;

    @Autowired
    private MovimentiService servizioMovimenti;

    @Autowired
    private OrdineService ordini;

    @Autowired
    private ProdottoRepository prodotti;

    @Autowired
    private MovimentoRepository movimenti;

    @BeforeEach
    void preparaProdotto() {
        prodotti.save(new Prodotto("MOV-TEST", "Prodotto movimenti", new BigDecimal("2.50"), 0, 0));
    }

    @Test
    void riassortisceUnoEMillePezziEAccettaUnaNotaDiDuecentoCaratteri() {
        String nota = "n".repeat(200);
        catalogo.riassortisci("MOV-TEST", 1, "Consegna");
        catalogo.riassortisci("MOV-TEST", 1000, nota);

        assertEquals(1001, prodotti.findByCodice("MOV-TEST").orElseThrow().getDisponibili());
        List<MovimentoMagazzino> registrati = movimenti.findAll();
        assertEquals(2, registrati.size());
        assertEquals(TipoMovimento.CARICO, registrati.get(1).getTipo());
        assertEquals(1000, registrati.get(1).getQuantita());
        assertEquals(nota, registrati.get(1).getNota());
        assertEquals("MOV-TEST", registrati.get(1).getProdotto().getCodice());
        assertNotNull(registrati.get(1).getDataOra());
    }

    @Test
    void quantitaZeroOMilleunoENotaDiDuecentounoCaratteriSonoRifiutatiSenzaMovimenti() {
        assertThrows(RichiestaNonValidaException.class, () -> catalogo.riassortisci("MOV-TEST", 0, null));
        assertThrows(RichiestaNonValidaException.class, () -> catalogo.riassortisci("MOV-TEST", 1001, null));
        assertThrows(RichiestaNonValidaException.class,
                () -> catalogo.riassortisci("MOV-TEST", 1, "n".repeat(201)));

        assertEquals(0, prodotti.findByCodice("MOV-TEST").orElseThrow().getDisponibili());
        assertEquals(0, movimenti.count());
    }

    @Test
    void creazioneEAnnullamentoRegistranoUnMovimentoPerRigaConNumeroOrdine() {
        prodotti.save(new Prodotto("MOV-ORD-A", "Prodotto ordine A", new BigDecimal("2.50"), 10, 0));
        prodotti.save(new Prodotto("MOV-ORD-B", "Prodotto ordine B", new BigDecimal("5.00"), 10, 0));
        var ordine = ordini.crea(new NuovoOrdine("Cliente movimenti", List.of(
                new NuovoOrdine.Riga("MOV-ORD-A", 2),
                new NuovoOrdine.Riga("MOV-ORD-A", 3),
                new NuovoOrdine.Riga("MOV-ORD-B", 1))));

        List<MovimentoMagazzino> scarichi = movimenti.findAll().stream()
                .filter(m -> m.getTipo() == TipoMovimento.SCARICO)
                .toList();
        assertEquals(2, scarichi.size());
        assertEquals(ordine.getId(), scarichi.get(0).getNumeroOrdine());
        assertEquals(5, scarichi.stream().filter(m -> m.getProdotto().getCodice().equals("MOV-ORD-A"))
                .findFirst().orElseThrow().getQuantita());

        ordini.annulla(ordine.getId());

        List<MovimentoMagazzino> resi = movimenti.findAll().stream()
                .filter(m -> m.getTipo() == TipoMovimento.RESO)
                .toList();
        assertEquals(2, resi.size());
        assertEquals(ordine.getId(), resi.get(0).getNumeroOrdine());
        assertEquals(10, prodotti.findByCodice("MOV-ORD-A").orElseThrow().getDisponibili());
        assertEquals(10, prodotti.findByCodice("MOV-ORD-B").orElseThrow().getDisponibili());
    }

    @Test
    void elencoOrdinaPerDataPiuRecenteEIdInCasoDiParita() {
        catalogo.riassortisci("MOV-TEST", 1, "Primo");
        catalogo.riassortisci("MOV-TEST", 1, "Secondo");

        List<MovimentoMagazzino> elenco = servizioMovimenti.elenco();

        assertEquals("Secondo", elenco.get(0).getNota());
        assertEquals("Primo", elenco.get(1).getNota());
    }
}
