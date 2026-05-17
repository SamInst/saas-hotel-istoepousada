package saas.hotel.istoepousada.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import saas.hotel.istoepousada.dto.CalcularPreco;
import saas.hotel.istoepousada.service.CalcularPrecoService;

import java.util.List;

@RestController
@RequestMapping("/calcular-preco")
public class CalcularPrecoController {
    private final CalcularPrecoService calcularPrecoService;

    public CalcularPrecoController(CalcularPrecoService calcularPrecoService) {
        this.calcularPrecoService = calcularPrecoService;
    }

    @PostMapping
    public List<CalcularPreco.Resultado> calcularPreco(
            @RequestBody List<CalcularPreco.Request> requests) {
        return calcularPrecoService.calcularPreco(requests);
    }
}
