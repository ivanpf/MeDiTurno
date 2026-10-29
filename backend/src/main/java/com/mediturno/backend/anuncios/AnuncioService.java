package com.mediturno.backend.anuncios;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.mediturno.backend.anuncios.dto.AltaAnuncioDTO;
import com.mediturno.backend.anuncios.dto.AnuncioResponseDTO;
import com.mediturno.backend.anuncios.dto.ArchivoImagenDTO;
import com.mediturno.backend.anuncios.dto.ImagenAnuncioResponseDTO;
import com.mediturno.backend.anuncios.model.Anuncio;
import com.mediturno.backend.anuncios.model.EstadoAnuncio;
import com.mediturno.backend.anuncios.model.ImagenAnuncio;
import com.mediturno.backend.anuncios.repository.AnuncioRepository;
import com.mediturno.backend.anuncios.repository.ImagenAnuncioRepository;
import com.mediturno.backend.common.exception.NoImplementadoException;
import com.mediturno.backend.usuarios.repository.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio de: CU-6, CU-17, CU-18.
 * Los métodos de CU-6 y CU-18 todavía no están implementados: lanzan NoImplementadoException (respuesta 501).
 */
@Service
public class AnuncioService {

    private static final byte[] FIRMA_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private final AnuncioRepository anuncioRepository;
    private final ImagenAnuncioRepository imagenAnuncioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final Clock reloj;
    private final long tamanoMaximoBytes;

    public AnuncioService(AnuncioRepository anuncioRepository, ImagenAnuncioRepository imagenAnuncioRepository,
            FuncionarioRepository funcionarioRepository, Clock reloj,
            @Value("${mediturno.anuncios.tamano-maximo-bytes}") long tamanoMaximoBytes) {
        this.anuncioRepository = anuncioRepository;
        this.imagenAnuncioRepository = imagenAnuncioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.reloj = reloj;
        this.tamanoMaximoBytes = tamanoMaximoBytes;
    }

    /** CU-6. */
    public List<AnuncioResponseDTO> listarCartelera() {
        throw new NoImplementadoException();
    }

    /** CU-17. */
    @Transactional
    public AnuncioResponseDTO publicarAnuncio(Long idFuncionario, AltaAnuncioDTO datos, MultipartFile archivo) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate inicio = datos.fechaInicio() != null ? datos.fechaInicio() : hoy;
        validarVigencia(inicio, datos.fechaFin(), hoy);
        ImagenAnuncio imagen = archivo == null ? null : validarImagen(archivo, datos.textoAlternativo());

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Anuncio anuncio = new Anuncio();
        anuncio.setFuncionario(funcionarioRepository.getReferenceById(idFuncionario));
        anuncio.setTitulo(datos.titulo().trim());
        anuncio.setContenido(datos.contenido().trim());
        anuncio.setFechaCreacion(ahora);
        anuncio.setFechaPublicacion(ahora);
        anuncio.setFechaInicio(inicio);
        anuncio.setFechaFin(datos.fechaFin());
        anuncio.setAnuncioEstado(EstadoAnuncio.PUBLICADO);
        anuncioRepository.save(anuncio);

        if (imagen != null) {
            imagen.setAnuncio(anuncio);
            imagenAnuncioRepository.save(imagen);
        }
        return convertirAAnuncioResponseDTO(anuncio, imagen);
    }

    /** CU-17 / CU-6. El archivo de una imagen; no se entregan las de anuncios eliminados. */
    @Transactional(readOnly = true)
    public ArchivoImagenDTO obtenerImagen(Long idImagen) {
        ImagenAnuncio imagen = imagenAnuncioRepository.buscarSalvoEstado(idImagen, EstadoAnuncio.ELIMINADO)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La imagen no existe"));
        return new ArchivoImagenDTO(imagen.getDatos(), imagen.getTipoContenido());
    }

    /** CU-18. */
    public void eliminarAnuncio(Long idAnuncio) {
        throw new NoImplementadoException();
    }

    private void validarVigencia(LocalDate inicio, LocalDate fin, LocalDate hoy) {
        if (inicio.isBefore(hoy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio no puede ser anterior a hoy");
        }
        if (fin != null && fin.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    /** Comprueba tamaño y formato (por el contenido del archivo, no por su nombre) y arma la imagen a guardar. */
    private ImagenAnuncio validarImagen(MultipartFile archivo, String texto) {
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "imagen";
        if (archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen «" + nombre + "» está vacía");
        }
        if (archivo.getSize() > tamanoMaximoBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La imagen «" + nombre + "» supera el tamaño máximo de " + tamanoMaximoBytes / (1024 * 1024) + " MB");
        }
        byte[] bytes = leer(archivo);
        String tipo = tipoPorContenido(bytes);
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen «" + nombre + "» debe ser JPG o PNG");
        }
        ImagenAnuncio imagen = new ImagenAnuncio();
        imagen.setDatos(bytes);
        imagen.setNombreArchivo(nombre.length() > 255 ? nombre.substring(0, 255) : nombre);
        imagen.setTipoContenido(tipo);
        imagen.setTextoAlternativo(texto == null || texto.isBlank() ? null : texto.trim());
        return imagen;
    }

    private byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen «" + archivo.getOriginalFilename() + "»");
        }
    }

    private static String tipoPorContenido(byte[] bytes) {
        if (empiezaCon(bytes, FIRMA_JPG)) {
            return "image/jpeg";
        }
        if (empiezaCon(bytes, FIRMA_PNG)) {
            return "image/png";
        }
        return null;
    }

    private static boolean empiezaCon(byte[] bytes, byte[] firma) {
        if (bytes.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (bytes[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    private AnuncioResponseDTO convertirAAnuncioResponseDTO(Anuncio anuncio, ImagenAnuncio imagen) {
        ImagenAnuncioResponseDTO imagenDTO = imagen == null ? null
            : new ImagenAnuncioResponseDTO(imagen.getIdImagenAnuncio(), imagen.getNombreArchivo(), imagen.getTextoAlternativo());
        return new AnuncioResponseDTO(anuncio.getIdAnuncio(), anuncio.getTitulo(), anuncio.getContenido(),
            anuncio.getFechaPublicacion(), anuncio.getFechaInicio(), anuncio.getFechaFin(), anuncio.getAnuncioEstado(), imagenDTO);
    }
}
