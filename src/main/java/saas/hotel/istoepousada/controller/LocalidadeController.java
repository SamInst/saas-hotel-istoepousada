package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.dto.Endereco;
import saas.hotel.istoepousada.service.EnderecoService;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class LocalidadeController {

    private final EnderecoService enderecoService;

    public LocalidadeController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<Empresa.EmpresaResponse> buscarPorCnpj(@PathVariable String cnpj) {
        Empresa.EmpresaResponse response = enderecoService.buscarEmpresaPorCnpj(cnpj);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cep/{cep}")
    public ResponseEntity<Endereco> buscarPorCep(
            @Parameter(
                    description = "CEP a ser consultado (com ou sem formatação)",
                    example = "65066-260",
                    required = true)
            @PathVariable
            String cep) {
        Endereco response = enderecoService.buscarEnderecoPorCep(cep);
        return ResponseEntity.ok(response);
    }
}