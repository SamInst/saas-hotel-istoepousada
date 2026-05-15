package saas.hotel.istoepousada.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.hotel.istoepousada.service.HospedagemService;

import java.time.LocalDate;

@RestController
@RequestMapping("/hospedagem")
public class HospedagemController {
    private final HospedagemService hospedagemService;

    public HospedagemController(HospedagemService hospedagemService) {
        this.hospedagemService = hospedagemService;
    }

//    public float calcularValorTotal(
//            Long categoriaId,
//            LocalDate dataEntrada,
//            int diarias,
//            int quantidadePessoas) {
//        return hospedagemService.
//    }
}
