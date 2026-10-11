package com.mediturno.backend.anuncios;

import java.util.List;

import com.mediturno.backend.anuncios.dto.AltaAnuncioDTO;
import com.mediturno.backend.anuncios.dto.AnuncioResponseDTO;
import com.mediturno.backend.common.exception.NoImplementadoException;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de: CU-6, CU-17, CU-18.
 * Los métodos todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class AnuncioService {

    /** CU-6. */
    public List<AnuncioResponseDTO> listarCartelera() {
        throw new NoImplementadoException();
    }

    /** CU-17. */
    public AnuncioResponseDTO publicarAnuncio(AltaAnuncioDTO datos) {
        throw new NoImplementadoException();
    }

    /** CU-18. */
    public void eliminarAnuncio(Long idAnuncio) {
        throw new NoImplementadoException();
    }
}
