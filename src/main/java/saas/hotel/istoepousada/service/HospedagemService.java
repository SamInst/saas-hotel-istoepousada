package saas.hotel.istoepousada.service;

import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.repository.HospedagemRepository;

@Service
public class HospedagemService {
    private final HospedagemRepository hospedagemRepository;
    private final CategoriaService categoriaService;

    public HospedagemService(HospedagemRepository hospedagemRepository, CategoriaService categoriaService) {
        this.hospedagemRepository = hospedagemRepository;
        this.categoriaService = categoriaService;
    }



}
