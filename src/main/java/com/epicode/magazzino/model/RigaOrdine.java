package com.epicode.magazzino.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Un prodotto dentro un ordine, con il prezzo fissato al momento dell'ordine.
 */
@Entity
public class RigaOrdine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Ordine ordine;

    @ManyToOne(optional = false)
    private Prodotto prodotto;

    private int quantita;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoUnitario;

    protected RigaOrdine() {
        // per JPA
    }

    RigaOrdine(Ordine ordine, Prodotto prodotto, int quantita) {
        this.ordine = ordine;
        this.prodotto = prodotto;
        this.quantita = quantita;
        this.prezzoUnitario = prodotto.getPrezzo();
    }

    void aumenta(int altri) {
        quantita += altri;
    }

    /**
     * La fascia dipende dalla quantita' complessiva, anche dopo aggiunte
     * successive.
     */
    public int getPercentualeSconto() {
        // La fascia maggiore va verificata prima di quella intermedia.
        if (quantita >= 50) {
            return 10;
        }
        if (quantita >= 11) {
            return 15;
        }
        return 0;
    }

    /** Subtotale scontato, arrotondato al centesimo soltanto alla fine. */
    public BigDecimal subtotale() {
        // Il fattore decimale esatto evita arrotondamenti intermedi dello sconto.
        BigDecimal fattoreScontato = BigDecimal.valueOf(100 - getPercentualeSconto(), 2);
        return prezzoUnitario.multiply(BigDecimal.valueOf(quantita))
                .multiply(fattoreScontato)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Prodotto getProdotto() {
        return prodotto;
    }

    public int getQuantita() {
        return quantita;
    }

    public BigDecimal getPrezzoUnitario() {
        return prezzoUnitario;
    }
}
