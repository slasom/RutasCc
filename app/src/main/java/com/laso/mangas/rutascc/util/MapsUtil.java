package com.laso.mangas.rutascc.util;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.laso.mangas.rutascc.model.Ruta;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsUtil {

    /**
     * Variable para almacenar la duración de la ruta calculada.
     */
    private String duration;

    public String getDuration() {
        return duration;
    }

    /**
     * Método para parsear la ruta obtenida de la api de Google Maps.
     *
     * @param response
     * @return
     */
    public List<List<HashMap<String, String>>> parseRoute(JSONObject response) {

        //Este método PARSEA el JSONObject que retorna del API de Rutas de Google devolviendo
        //una lista del lista de HashMap Strings con el listado de Coordenadas de Lat y Long,
        //con la cual se podrá dibujar pollinas que describan la ruta entre 2 puntos.


        List<List<HashMap<String, String>>> routes = new ArrayList<>();

        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;
        JSONObject jDuration = null;

        //  String duration;


        try {

            jRoutes = response.getJSONArray("routes");

            /** Traversing all routes */
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                List<HashMap<String, String>> path = new ArrayList<HashMap<String, String>>();
                /** Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");
                    jDuration = (JSONObject) ((JSONObject) jLegs.get(j)).get("duration");

                    duration = (String) jDuration.get("text");


                    /** Traversing all steps */
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = "";
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        /** Traversing all points */
                        for (int l = 0; l < list.size(); l++) {
                            HashMap<String, String> hm = new HashMap<String, String>();
                            hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                            hm.put("lng", Double.toString(((LatLng) list.get(l)).longitude));
                            path.add(hm);

                        }
                    }

                    routes.add(path);

                }
            }
            return routes;

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }

        return routes;
    }

    /**
     * Método para pintar en el mapa las rutas obtenidas de la API de Google.
     * Pintará de azul la ruta calculada "andando" y de color verde en "bicicleta". Si no fuese ninguno
     * de estos métodos la pintará de amarillo.
     *
     * @param routes
     * @param mode
     * @return
     */
    public PolylineOptions drawPathFromGoogleResults(List<List<HashMap<String, String>>> routes, String mode) {
        ArrayList<LatLng> points = null;
        PolylineOptions polyLineOptions = null;

        // traversing through routes
        for (int i = 0; i < routes.size(); i++) {
            points = new ArrayList<LatLng>();
            polyLineOptions = new PolylineOptions();
            List<HashMap<String, String>> path = routes.get(i);

            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }

            polyLineOptions.addAll(points);
            polyLineOptions.width(4);


            if (mode.equals("walking"))
                polyLineOptions.color(Color.BLUE);
            else if (mode.equals("bicycling"))
                polyLineOptions.color(Color.rgb(0, 100, 0));
            else
                polyLineOptions.color(Color.YELLOW);
        }

        return polyLineOptions;
    }

    /**
     * Método para pintar las rutas obtenidas de OpenData Cáceres.
     *
     * @param puntosRuta
     * @return
     */
    public PolylineOptions drawPathFromLocalRoute(List<LatLng> puntosRuta) {
        ArrayList<LatLng> points = new ArrayList<>();
        PolylineOptions polyLineOptions = new PolylineOptions();


        for (int j = 0; j < puntosRuta.size(); j++) {


            points.add(puntosRuta.get(j));
        }

        polyLineOptions.addAll(points);
        polyLineOptions.width(8);


        polyLineOptions.color(Color.RED);


        return polyLineOptions;
    }


    /**
     * Método para generar la url de petición hacia la API de Google.
     * Genera la url con respecto desde la posición del dispositivo y el inicio (primer punto) de la
     * ruta pasada por parámetro
     *
     * Formato: "https://maps.googleapis.com/maps/api/directions/json?origin="+latitudInicial+","+longitudInicial
     *                 +"&destination="+latitudFinal+","+longitudFinal&mode="MODO"&key="API-KEY";
     *
     * @param r
     * @param posicion
     * @param mode
     * @return
     */
    public String getMapsApiDirectionsUrl(Ruta r, LatLng posicion, String mode) {

        String output = "json";

        String origin = "origin=" + posicion.latitude + "," + posicion.longitude;
        String destination = "destination=" + r.getPuntosRuta().get(0).latitude + ","
                + r.getPuntosRuta().get(0).longitude;


        String key = "key=AIzaSyD_vzCRU-kP5M4SiqNr9MuEyKKuGf6AIOs";
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + origin + "&" + destination + "&mode=" + mode + "&" + key;
        return url;
    }


    /**
     * @param encoded
     * @return
     */
    public List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
