package com.epicode.magazzino.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class MovimentoMagazzino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataOra;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Prodotto prodotto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimento tipo;

    @Column(nullable = false)
    private int quantita;

    @Column(length = 200)
    private String nota;

    private Long numeroOrdine;

    protected MovimentoMagazzino() {
        // per JPA
    }

    public MovimentoMagazzino(LocalDateTime dataOra, Prodotto prodotto, TipoMovimento tipo, int quantita,
                              String nota, Long numeroOrdine) {
        if (dataOra == null || prodotto == null || tipo == null || quantita <= 0) {
            throw new RichiestaNonValidaException("I dati del movimento non sono validi");
        }
        if (tipo == TipoMovimento.CARICO && quantita > 1000) {
            throw new RichiestaNonValidaException("Il riassortimento deve essere compreso tra 1 e 1000 pezzi");
        }
        if (nota != null && nota.length() > 200) {
            throw new RichiestaNonValidaException("La nota non può superare 200 caratteri");
        }
        if (tipo != TipoMovimento.CARICO && numeroOrdine == null) {
            throw new RichiestaNonValidaException("Il numero d'ordine è obbligatorio per questo movimento");
        }
        this.dataOra = dataOra;
        this.prodotto = prodotto;
        this.tipo = tipo;
        this.quantita = quantita;
        this.nota = nota;
        this.numeroOrdine = numeroOrdine;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataOra() {
        return dataOra;
    }

    public Prodotto getProdotto() {
        return prodotto;
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public int getQuantita() {
        return quantita;
    }

    public String getNota() {
        return nota;
    }

    public Long getNumeroOrdine() {
        return numeroOrdine;
    }
}
