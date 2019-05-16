package com.laso.mangas.rutascc.util;


import com.google.android.gms.maps.model.LatLng;
import com.laso.mangas.rutascc.model.Ruta;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SparqlUtil {


    /**
     * Método para parsear las Rutas (Patrimonio, Natural y Verde) obtenidas de OpenData Caceres.
     *
     * @param response
     * @param rutas
     */
    public void paserRouteSparql(JSONObject response, List<Ruta> rutas) {

        List<LatLng> puntosRuta = new ArrayList<>();

        JSONObject jResults = null;
        JSONArray jBindings = null;

        JSONObject jRuta = null;
        JSONObject jNombreRuta = null;

        JSONObject jPathRuta = null;


        try {

            jResults = response.getJSONObject("results");
            jBindings = jResults.getJSONArray("bindings");

            /** Traversing all legs */
            for (int j = 0; j < jBindings.length(); j++) {
                jRuta = (JSONObject) jBindings.get(j);
                jNombreRuta = (JSONObject) jRuta.get("nombreRuta");

                Ruta r = new Ruta(jNombreRuta.getString("value"));

                jPathRuta = (JSONObject) jRuta.get("ruta");

                String paths = (String) jPathRuta.getString("value");

                String[] args = paths.split("]");

                for (int k = 0; k < args.length; k++) {

                    String[] puntos = args[k].substring(2).split(",");
                    puntosRuta.add(new LatLng(Double.parseDouble(puntos[1]), Double.parseDouble(puntos[0])));
                }

                r.setPuntosRuta(puntosRuta);
                puntosRuta = new ArrayList<>();
                rutas.add(r);

            }


        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }

    }


    /**
     * Método para parsear las Rutas (Patrimonio, Natural y Verde) en la que incluye el nombre de la ruta
     * y el punto cercano a la posicion del dispositivo .
     *
     * @param response
     * @return
     */
    public List<String> paserRutasCercanas(JSONObject response) {

        List<String> rutasCercanas = new ArrayList<>();

        JSONObject jResults = null;
        JSONArray jBindings = null;

        JSONObject jRuta = null;
        JSONObject jNombreRuta = null;
        JSONObject jPuntoLat = null;
        JSONObject jPuntoLng = null;


        try {

            jResults = response.getJSONObject("results");
            jBindings = jResults.getJSONArray("bindings");

            /** Traversing all legs */
            for (int j = 0; j < jBindings.length(); j++) {
                jRuta = (JSONObject) jBindings.get(j);
                jNombreRuta = (JSONObject) jRuta.get("nombreRuta");
                jPuntoLat = (JSONObject) jRuta.get("puntoLat");
                jPuntoLng = (JSONObject) jRuta.get("puntoLng");


                String nombreRuta = jNombreRuta.getString("value");
                Double puntoLat = jPuntoLat.getDouble("value");
                Double puntoLng = jPuntoLng.getDouble("value");

                rutasCercanas.add(nombreRuta);

            }
            return rutasCercanas;

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
        return rutasCercanas;
    }


}
