package com.laso.mangas.rutascc.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase para almacenar una Ruta y sus diferentes puntos de coordenadas.
 */
public class Ruta {


    private String nombreRuta;
    private List<LatLng> puntosRuta;

    public Ruta(String nombreRuta) {
        this.nombreRuta = nombreRuta;
    }

    public Ruta(String nombreRuta, List<LatLng> puntosRuta) {
        this.nombreRuta = nombreRuta;
        this.puntosRuta = puntosRuta;
    }

    public String getNombreRuta() {
        return nombreRuta;
    }

    public void setNombreRuta(String nombreRuta) {
        this.nombreRuta = nombreRuta;
    }

    public List<LatLng> getPuntosRuta() {
        return puntosRuta;
    }

    public void setPuntosRuta(List<LatLng> puntosRuta) {
        this.puntosRuta = puntosRuta;
    }

    @Override
    public String toString() {
        return "Ruta{" +
                "nombreRuta='" + nombreRuta;
    }
}
