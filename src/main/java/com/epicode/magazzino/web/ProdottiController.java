package com.epicode.magazzino.web;

import com.epicode.magazzino.service.CatalogoService;
import com.epicode.magazzino.model.MagazzinoException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProdottiController {

    private final CatalogoService catalogo;

    public ProdottiController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/prodotti";
    }

    @GetMapping("/prodotti")
    public String elenco(Model model) {
        model.addAttribute("prodotti", catalogo.elenco());
        return "prodotti";
    }

    @GetMapping("/prodotti/{codice}/riassortimento")
    public String riassortimento(@PathVariable String codice, Model model) {
        model.addAttribute("prodotto", catalogo.trova(codice));
        model.addAttribute("form", new RiassortimentoForm());
        return "riassortimento";
    }

    @PostMapping("/prodotti/{codice}/riassortimento")
    public String riassortisci(@PathVariable String codice, @ModelAttribute("form") RiassortimentoForm form,
                               BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("errore", "Inserisci un numero intero di pezzi valido");
            model.addAttribute("prodotto", catalogo.trova(codice));
            return "riassortimento";
        }
        try {
            catalogo.riassortisci(codice, form.getPezzi() == null ? 0 : form.getPezzi(), form.getNota());
            redirect.addFlashAttribute("messaggio", "Riassortimento registrato");
            return "redirect:/prodotti";
        } catch (MagazzinoException e) {
            model.addAttribute("errore", e.getMessage());
            model.addAttribute("prodotto", catalogo.trova(codice));
            return "riassortimento";
        }
    }
}
