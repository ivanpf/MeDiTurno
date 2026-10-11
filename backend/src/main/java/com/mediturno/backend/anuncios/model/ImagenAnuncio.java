package com.mediturno.backend.anuncios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "imagen_anuncio")
public class ImagenAnuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen_anuncio")
    private Long idImagenAnuncio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_anuncio", nullable = false)
    private Anuncio anuncio;

    @Column(name = "imagen", nullable = false, length = 500)
    private String imagen;

    @Column(name = "orden", nullable = false)
    private Integer orden;

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

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
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
