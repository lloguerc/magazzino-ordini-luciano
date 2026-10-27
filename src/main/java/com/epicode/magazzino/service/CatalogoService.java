package com.epicode.magazzino.service;

import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.RichiestaNonValidaException;
import com.epicode.magazzino.model.RisorsaNonTrovataException;
import com.epicode.magazzino.repository.ProdottoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogoService {

    private final ProdottoRepository prodotti;
    private final MovimentiService movimenti;

    public CatalogoService(ProdottoRepository prodotti, MovimentiService movimenti) {
        this.prodotti = prodotti;
        this.movimenti = movimenti;
    }

    @Transactional(readOnly = true)
    public List<Prodotto> elenco() {
        return prodotti.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Prodotto trova(String codice) {
        return prodotti.findByCodice(codice)
                .orElseThrow(() -> new RisorsaNonTrovataException("Prodotto " + codice + " non trovato"));
    }

    public Prodotto riassortisci(String codice, int quantita, String nota) {
        if (quantita < 1 || quantita > 1000) {
            throw new RichiestaNonValidaException("Il riassortimento deve essere compreso tra 1 e 1000 pezzi");
        }
        if (nota != null && nota.length() > 200) {
            throw new RichiestaNonValidaException("La nota non può superare 200 caratteri");
        }
        Prodotto prodotto = trova(codice);
        prodotto.riassortisci(quantita);
        movimenti.registraCarico(prodotto, quantita, nota);
        return prodotto;
    }

    public Prodotto riassortisci(String codice, int quantita) {
        return riassortisci(codice, quantita, null);
    }
}
