package com.mediturno.backend.anuncios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "imagen_anuncio")
public class ImagenAnuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen_anuncio")
    private Long idImagenAnuncio;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_anuncio", nullable = false, unique = true)
    private Anuncio anuncio;

    @Column(name = "datos", nullable = true)
    private byte[] datos;

    @Column(name = "nombre_archivo", nullable = true, length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = true, length = 100)
    private String tipoContenido;

    @Column(name = "texto_alternativo", nullable = true, length = 255)
    private String textoAlternativo;

    public Long getIdImagenAnuncio() {
        return idImagenAnuncio;
    }

    public void setIdImagenAnuncio(Long idImagenAnuncio) {
        this.idImagenAnuncio = idImagenAnuncio;
    }

    public Anuncio getAnuncio() {
        return anuncio;
    }

    public void setAnuncio(Anuncio anuncio) {
        this.anuncio = anuncio;
    }

    public byte[] getDatos() {
        return datos;
    }

    public void setDatos(byte[] datos) {
        this.datos = datos;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public void setTipoContenido(String tipoContenido) {
        this.tipoContenido = tipoContenido;
    }

    public String getTextoAlternativo() {
        return textoAlternativo;
    }

    public void setTextoAlternativo(String textoAlternativo) {
        this.textoAlternativo = textoAlternativo;
    }
}
