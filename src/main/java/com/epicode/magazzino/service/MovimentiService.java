package com.epicode.magazzino.service;

import com.epicode.magazzino.model.MovimentoMagazzino;
import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.TipoMovimento;
import com.epicode.magazzino.repository.MovimentoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MovimentiService {

    private final MovimentoRepository movimenti;
    private final Clock clock;

    public MovimentiService(MovimentoRepository movimenti, Clock clock) {
        this.movimenti = movimenti;
        this.clock = clock;
    }

    public void registraCarico(Prodotto prodotto, int quantita, String nota) {
        registra(prodotto, TipoMovimento.CARICO, quantita, nota, null);
    }

    public void registraScarico(Prodotto prodotto, int quantita, Long numeroOrdine) {
        registra(prodotto, TipoMovimento.SCARICO, quantita, null, numeroOrdine);
    }

    public void registraReso(Prodotto prodotto, int quantita, Long numeroOrdine) {
        registra(prodotto, TipoMovimento.RESO, quantita, null, numeroOrdine);
    }

    @Transactional(readOnly = true)
    public List<MovimentoMagazzino> elenco() {
        return movimenti.findAllByOrderByDataOraDescIdDesc();
    }

    private void registra(Prodotto prodotto, TipoMovimento tipo, int quantita, String nota, Long numeroOrdine) {
        movimenti.save(new MovimentoMagazzino(LocalDateTime.now(clock), prodotto, tipo, quantita, nota, numeroOrdine));
    }
}
