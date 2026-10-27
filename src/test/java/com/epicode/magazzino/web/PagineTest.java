package com.epicode.magazzino.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epicode.magazzino.model.Ordine;
import com.epicode.magazzino.model.MovimentoMagazzino;
import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.TipoMovimento;
import com.epicode.magazzino.repository.MovimentoRepository;
import com.epicode.magazzino.repository.ProdottoRepository;
import com.epicode.magazzino.service.CatalogoService;
import com.epicode.magazzino.service.NuovoOrdine;
import com.epicode.magazzino.service.OrdineService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Le pagine principali si aprono e il form crea davvero un ordine. */
@SpringBootTest
@AutoConfigureMockMvc
class PagineTest {

    @Autowired
    private MockMvc mvc;

    // I nuovi test creano ordini con prodotti dedicati, senza mutare il catalogo
    // demo.
    @Autowired
    private OrdineService ordini;

    @Autowired
    private ProdottoRepository prodotti;

    @Autowired
    private MovimentoRepository movimenti;

    @Autowired
    private CatalogoService catalogo;

    @Test
    void elencoProdotti() throws Exception {
        mvc.perform(get("/prodotti"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trapano a batteria 18V")))
                .andExpect(content().string(containsString("Riassortisci")))
                .andExpect(content().string(containsString("/prodotti/GUA-L/riassortimento")));
    }

    @Test
    @Transactional
    void ilFormDiRiassortimentoAccettaIlMassimoERegistraIlCarico() throws Exception {
        prodotti.save(new Prodotto("WEB-CARICO", "Prodotto carico web", new BigDecimal("1.00"), 5, 0));

        mvc.perform(get("/prodotti/WEB-CARICO/riassortimento"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("max=\"1000\"")))
                .andExpect(content().string(containsString("maxlength=\"200\"")));
        mvc.perform(post("/prodotti/WEB-CARICO/riassortimento")
                .param("pezzi", "1000")
                .param("nota", "n".repeat(200)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prodotti"));

        assertEquals(1005, prodotti.findByCodice("WEB-CARICO").orElseThrow().getDisponibili());
        MovimentoMagazzino movimento = movimenti.findAll().stream()
                .filter(m -> m.getProdotto().getCodice().equals("WEB-CARICO"))
                .findFirst().orElseThrow();
        assertEquals(TipoMovimento.CARICO, movimento.getTipo());
        assertEquals(1000, movimento.getQuantita());
        assertEquals("n".repeat(200), movimento.getNota());
    }

    @Test
    @Transactional
    void formRifiutaValoriNonValidiSenzaModificareLaGiacenza() throws Exception {
        prodotti.save(new Prodotto("WEB-ERRORE", "Prodotto errore web", new BigDecimal("1.00"), 5, 0));

        mvc.perform(post("/prodotti/WEB-ERRORE/riassortimento").param("pezzi", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("compreso tra 1 e 1000 pezzi")));
        mvc.perform(post("/prodotti/WEB-ERRORE/riassortimento").param("pezzi", "non-intero"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("numero intero di pezzi valido")));
        mvc.perform(post("/prodotti/WEB-ERRORE/riassortimento")
                .param("pezzi", "1")
                .param("nota", "n".repeat(201)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La nota non può superare 200 caratteri")));

        assertEquals(5, prodotti.findByCodice("WEB-ERRORE").orElseThrow().getDisponibili());
        assertEquals(0, movimenti.findAll().stream()
                .filter(m -> m.getProdotto().getCodice().equals("WEB-ERRORE"))
                .count());
    }

    @Test
    @Transactional
    void laPaginaMovimentiEIlMenuSonoDisponibili() throws Exception {
        prodotti.save(new Prodotto("WEB-MOV", "Prodotto pagina movimenti", new BigDecimal("1.00"), 0, 0));
        catalogo.riassortisci("WEB-MOV", 1, "Arrivo");

        mvc.perform(get("/movimenti"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Movimenti")))
                .andExpect(content().string(containsString("Prodotto pagina movimenti")))
                .andExpect(content().string(containsString("Arrivo")));
    }

    @Test
    void ilFormCreaUnOrdineEPortaAlDettaglio() throws Exception {
        mvc.perform(post("/ordini")
                .param("cliente", "Ferramenta Bianchi")
                .param("righe[0].codice", "GUA-L")
                .param("righe[0].quantita", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/ordini/*"));
    }

    @Test
    void ilFormMostraLErroreSeLaScortaNonBasta() throws Exception {
        mvc.perform(post("/ordini")
                .param("cliente", "Ferramenta Bianchi")
                .param("righe[0].codice", "NAS-50")
                .param("righe[0].quantita", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Scorta insufficiente per NAS-50")));
    }

    @Test
    void ordineInesistente() throws Exception {
        mvc.perform(get("/ordini/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void dettaglioEdElencoMostranoLoScontoConsolidatoELaSpedizione() throws Exception {
        // Il rollback isola la fixture e la richiesta duplicata verifica lo sconto del
        // 5%.
        prodotti.save(new Prodotto("WEB-SCONTO", "Prodotto web sconto", new BigDecimal("2.50"), 20, 0));
        Ordine ordine = ordini.crea(new NuovoOrdine("Cliente web sconto", List.of(
                new NuovoOrdine.Riga("WEB-SCONTO", 5),
                new NuovoOrdine.Riga("WEB-SCONTO", 5))));

        // L'intera riga deve contenere quantita', prezzo, percentuale e netto coerenti.
        mvc.perform(get("/ordini/{id}", ordine.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Sconto</th>")))
                .andExpect(content().string(matchesPattern("(?s).*<tbody>\\s*<tr>\\s*"
                        + "<td>Prodotto web sconto</td>\\s*<td[^>]*>10</td>\\s*"
                        + "<td[^>]*>2,50 €</td>\\s*<td[^>]*>5%</td>\\s*"
                        + "<td[^>]*>23,75 €</td>\\s*</tr>\\s*</tbody>.*")));
        verificaRiepilogo(ordine.getId(), "23,75", "9,90", "33,65");
        verificaTotaleNellElenco(ordine.getId(), "Cliente web sconto", "33,65");
    }

    @Test
    @Transactional
    void lePagineMostranoLoScontoDelDieciPercentoELaSpedizioneGratuita() throws Exception {
        // Il netto di 50 pezzi resta sopra soglia e non deve ricevere la tariffa di
        // 9,90.
        prodotti.save(new Prodotto("WEB-GRATIS", "Prodotto web gratuito", new BigDecimal("3.34"), 50, 0));
        Ordine ordine = ordini.crea(new NuovoOrdine("Cliente web gratuito", List.of(
                new NuovoOrdine.Riga("WEB-GRATIS", 50))));
        mvc.perform(get("/ordini/{id}", ordine.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<td>Prodotto web gratuito</td>\\s*"
                        + "<td[^>]*>50</td>\\s*<td[^>]*>3,34 €</td>\\s*"
                        + "<td[^>]*>10%</td>\\s*<td[^>]*>150,30 €</td>.*")));
        verificaRiepilogo(ordine.getId(), "150,30", "0,00", "150,30");
        verificaTotaleNellElenco(ordine.getId(), "Cliente web gratuito", "150,30");
    }

    @Test
    @Transactional
    void ilDettaglioMostraZeroPercentoESpedizioneGratuitaAllaSogliaEsatta() throws Exception {
        // Un pezzo da 150 euro verifica insieme assenza di sconto e soglia inclusiva.
        prodotti.save(new Prodotto("WEB-SOGLIA", "Prodotto web soglia", new BigDecimal("150.00"), 1, 0));
        Ordine ordine = ordini.crea(new NuovoOrdine("Cliente web soglia", List.of(
                new NuovoOrdine.Riga("WEB-SOGLIA", 1))));
        mvc.perform(get("/ordini/{id}", ordine.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<td>Prodotto web soglia</td>\\s*"
                        + "<td[^>]*>1</td>\\s*<td[^>]*>150,00 €</td>\\s*"
                        + "<td[^>]*>0%</td>\\s*<td[^>]*>150,00 €</td>.*")));
        verificaRiepilogo(ordine.getId(), "150,00", "0,00", "150,00");
    }

    /**
     * Controlla le tre righe del riepilogo nel loro contesto, non importi sparsi
     * nella pagina.
     */
    private void verificaRiepilogo(Long identificativo, String merce, String spedizione, String totale)
            throws Exception {
        mvc.perform(get("/ordini/{id}", identificativo))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<tfoot>\\s*<tr>\\s*"
                        + "<th colspan=\"4\">Totale merce</th>\\s*<th[^>]*>" + merce + " €</th>\\s*</tr>\\s*"
                        + "<tr>\\s*<th colspan=\"4\">Spedizione</th>\\s*<th[^>]*>" + spedizione + " €</th>\\s*</tr>\\s*"
                        + "<tr>\\s*<th colspan=\"4\">Totale da pagare</th>\\s*<th[^>]*>" + totale
                        + " €</th>\\s*</tr>\\s*"
                        + "</tfoot>.*")));
    }

    /**
     * Collega il totale visualizzato all'ordine della fixture, anche in elenchi con
     * altri ordini.
     */
    private void verificaTotaleNellElenco(Long identificativo, String cliente, String totale) throws Exception {
        mvc.perform(get("/ordini"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Totale da pagare</th>")))
                .andExpect(content().string(matchesPattern("(?s).*<tr>\\s*<td><a href=\"/ordini/"
                        + identificativo + "\">" + identificativo + "</a></td>\\s*<td>" + cliente + "</td>\\s*"
                        + "<td>[^<]*</td>\\s*<td><span[^>]*>CONFERMATO</span></td>\\s*"
                        + "<td[^>]*>" + totale + " €</td>\\s*</tr>.*")));
    }
}
