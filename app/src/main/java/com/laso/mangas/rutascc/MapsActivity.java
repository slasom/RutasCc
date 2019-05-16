package com.laso.mangas.rutascc;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.laso.mangas.rutascc.model.Ruta;
import com.laso.mangas.rutascc.util.MapsUtil;
import com.laso.mangas.rutascc.util.SparqlUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    /**
     * Map Utils
     */
    private static final String TAG = "Location";
    private GoogleMap mMap;
    private LatLng miPosicion = new LatLng(0, 0);

    private static final LatLng CACERES = new LatLng(39.4693419, -6.3713693);

    //walking
    //bicycling
    private String travelMode = "walking";

    /**
     * Estructuras para almacenar rutas
     */
    private List<Ruta> rutasPatrimonio = new ArrayList<>();
    private List<Ruta> rutasNaturales = new ArrayList<>();
    private List<Ruta> rutasVerdes = new ArrayList<>();

    /**
     * Volley Utils
     */
    private JsonObjectRequest jsonObjectRequest;
    private RequestQueue request;


    /**
     * Layout Elements
     */
    private ImageButton buttonWalk;
    private ImageButton buttonBike;
    private final static String duration = "0 mins";
    private TextView infoRuta;

    private Spinner spinner;
    private Spinner spinnerTitulo;
    private List<Ruta> lSpinner = new ArrayList<>();
    private int posicionSpinner;


    /**
     * Utils
     */
    private MapsUtil mapsUtil = new MapsUtil();
    private SparqlUtil sparqlUtil = new SparqlUtil();


    /**
     * Notification Utils
     */
    private NotificationManager notificationManager;
    private PendingIntent intent;

    /*Parametros Rutas Cercanas*/
    private Timer timer;
    private long MILISECONDS_REFRESH = 1800000; // 30 min
    private double RANGE = 0.025; // 25 m


    /**
     * Tracking service
     */
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 13f;
    private boolean bandera = false;


    /**
     * Método onCreate del Activity.
     * <p>
     * En él se inicializan los diferentes elementos de la vista u otras variables y estructuras de
     * la aplicación.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        getLocationPermission();

        setUpNotification();

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();

        request = Volley.newRequestQueue(getApplicationContext());

        buttonWalk = (ImageButton) findViewById(R.id.imageButtonWalk);

        buttonBike = (ImageButton) findViewById(R.id.imageButtonBike);


        buttonWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocationPermissionsGranted) {
                    if (travelMode.equals("bicycling")) {
                        travelMode = "walking";
                        String url = mapsUtil.getMapsApiDirectionsUrl(lSpinner.get(posicionSpinner), miPosicion, travelMode);
                        webServiceObtenerRutaGoogle(url);
                        addMarkers(miPosicion, lSpinner.get(posicionSpinner).getPuntosRuta().get(0));

                    }
                }else
                    getLocationPermission();

            }
        });

        buttonBike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocationPermissionsGranted) {
                    if (travelMode.equals("walking")) {
                        travelMode = "bicycling";
                        String url = mapsUtil.getMapsApiDirectionsUrl(lSpinner.get(posicionSpinner), miPosicion, travelMode);
                        webServiceObtenerRutaGoogle(url);
                        addMarkers(miPosicion, lSpinner.get(posicionSpinner).getPuntosRuta().get(0));
                    }
                }else
                    getLocationPermission();
            }
        });


        infoRuta = (TextView) findViewById(R.id.textInfo);

        spinner = (Spinner) findViewById(R.id.spinner);
        spinnerTitulo = (Spinner) findViewById(R.id.spinnerTitulo);


    }


    /**
     * Método utilizado para chequear los permisos
     */
    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Método Callback de Android para comprobar si estan aceptados o no los permisos.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    /**
     * Método para inicializar el mapa.
     */
    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);


    }


    /**
     * Método OnResume de la actividad.
     */
    @Override
    protected void onResume() {
        super.onResume();

        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();

    }


    /**
     * Método para configurar el Spinner o Menú desplegable donde se encuentran los diferentes tipos de Rutas
     * que se encuentra en OpenData Cáceres.
     * <p>
     * En esta aplicación se muestran tres (Rutas Patrimonio, Rutas Naturales y Rutas Verdes)
     */
    private void loadSpinnerTitulo() {

        final ArrayList<String> nombreRutasSpinner = new ArrayList<>();

        nombreRutasSpinner.add("Rutas Patrimonio");
        nombreRutasSpinner.add("Rutas Naturales");
        nombreRutasSpinner.add("Rutas Verdes");

        spinnerTitulo.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, nombreRutasSpinner));

        spinnerTitulo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i("Seleccionado! ", parent.getItemAtPosition(position).toString());

                switch ((int) parent.getItemIdAtPosition(position)) {
                    case 0:
                        loadSpinner(rutasPatrimonio);
                        break;
                    case 1:
                        loadSpinner(rutasNaturales);
                        break;
                    case 2:
                        loadSpinner(rutasVerdes);
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }


    /**
     * Método de la API de Google que se ejecuta cuando el Mapa esta completamente cargado.
     * <p>
     * En el se comprobará si la localización ha sido obtenida y pondrá un punto azul con dicha localización.
     * También se configura para que nos muestre botón que nos mueve hacia la posición del dispositivo.
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CACERES, 14));

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

        }


    }


    /**
     * Método para obtener la localización del dispositivo.
     * Si obtiene la localización correctamente, enviará las peticiones para obtener las rutas y
     * que no se produzca ningún error posteriormente por falta de recursos.
     * <p>
     * También empieza un timer para comprobar cada X tiempo (MILISECONDS_REFRESH) si hay rutas cercanas.
     */
    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            miPosicion = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            if (!bandera) {
                                peticionSparql("RutaPatrimonio", rutasPatrimonio);
                                peticionSparql("RutaNatural", rutasNaturales);
                                peticionSparql("RutaVerde", rutasVerdes);


                                //Iniciar temporizador para avisos

                                timer.scheduleAtFixedRate(new TimerTask() {
                                    @Override
                                    public void run() {
                                        //TODO poner timer y poner a 0.025 = 25m
                                        peticionSparqlRutasCercanas(miPosicion, "RutaPatrimonio", String.valueOf(RANGE));
                                        peticionSparqlRutasCercanas(miPosicion, "RutaNatural", String.valueOf(RANGE));
                                        peticionSparqlRutasCercanas(miPosicion, "RutaVerde", String.valueOf(RANGE));


                                    }
                                }, 0, MILISECONDS_REFRESH);

                                bandera = true;
                            }

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM);

                        } else {
                            Log.e(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    /**
     * Método para mover automaticamente el mapa hacia el punto pasado por parámetro y un zoom concreto.
     *
     * @param latLng
     * @param zoom
     */
    private void moveCamera(LatLng latLng, float zoom) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }


    /**
     * Método para realizar la peticion hacia OpenData Caceres, con ella se obtienen las rutas
     * más cercanas dada la posición del dispositivo  y la distancia máxima permitida.
     * <p>
     * Si el resultado es correcto, enviará las notificaciones Push indicando dicho evento.
     *
     * @param posicion
     * @param recurso
     * @param distancia
     *
     * CONSULTA SPARQL
     *
     * select distinct ?nombreRuta (SAMPLE(?lngF) AS ?puntoLng) (SAMPLE(?latF) AS ?puntoLat)
     *  where {
     *      ?uri a om:RECURSO.
     *      ?uri rdfs:label ?nombreRuta.
     *      ?uri om:tienePunto ?puntos.
     *      ?puntos geo:lat ?latF.
     *      ?puntos geo:long ?lngF.
     *      filter (bif:st_distance ( bif:st_point (PUNTOLAT,PUNTOLONG) , bif:st_point (?lngF, ?latF) ) < DISTANCIA)
     *      }
     */
    private void peticionSparqlRutasCercanas(LatLng posicion, String recurso, String distancia) {

        String latitud = String.valueOf(posicion.latitude);
        String longitud = String.valueOf(posicion.longitude);


        String url = "http://opendata.caceres.es/sparql?default-graph-uri=&query=select%20distinct%20%3FnombreRuta%20(SAMPLE(%3FlngF)%20AS%20%3FpuntoLng)%20(SAMPLE(%3FlatF)%20AS%20%3FpuntoLat)%0Awhere%20%7B%0A%3Furi%20a%20om%3A" + recurso + ".%0A%3Furi%20rdfs%3Alabel%20%3FnombreRuta.%0A%3Furi%20om%3AtienePunto%20%3Fpuntos.%0A%3Fpuntos%20geo%3Alat%20%3FlatF.%0A%3Fpuntos%20geo%3Along%20%3FlngF.%0Afilter%20(bif%3Ast_distance%20(%20bif%3Ast_point%20(" + longitud + "%2C" + latitud + ")%20%2C%20bif%3Ast_point%20(%3FlngF%2C%20%3FlatF)%20)%20%3C%20" + distancia + ")%7D%0A%09&format=json";


        JsonObjectRequest requestSparql = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("RESULTADO SPARQL ", String.valueOf(response));
                List<String> rutasCercanas = sparqlUtil.paserRutasCercanas(response);
                sendNotification(rutasCercanas);


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("Error", String.valueOf(error));
            }
        });

        request.add(requestSparql);

    }

    /**
     * Método para realizar la peticion hacia OpenData Caceres, con ella se obtienen las rutas dado el
     * recurso por parametro(RutaPatrimonio, RutaNatural y RutaVerde).
     * <p>
     * Si el resultado es correcto, configurará y cargara el Spinner o Menu Desplegable con las rutas.
     *
     * CONSULTA SPARQL
     *  select ?nombreRuta ?ruta where{
     *      ?uri a om:RECURSO.
     *      ?uri rdfs:label ?nombreRuta.
     *      ?uri schema:line ?ruta.
     *  }
     *
     *
     * @param recurso
     * @param ruta
     */
    private void peticionSparql(String recurso, final List<Ruta> ruta) {


        String url = "https://opendata.caceres.es/sparql?default-graph-uri=&query=select%20%3FnombreRuta%20%3Fruta%20where%7B%0A%3Furi%20a%20om%3A" + recurso + ".%0A%3Furi%20rdfs%3Alabel%20%3FnombreRuta.%0A%3Furi%20schema%3Aline%20%3Fruta.%0A%7D%20%0A%09&format=json";

        JsonObjectRequest requestSparql = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //paserRoute(response,ruta);

                sparqlUtil.paserRouteSparql(response, ruta);
                loadSpinnerTitulo();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("Error", String.valueOf(error));
            }
        });

        request.add(requestSparql);

    }


    /**
     * Método para configurar el Spinner o Menú desplegable con las rutas obtenidas según su recurso.
     * <p>
     * Esta configurado para pintar en el mapa la ruta que se vaya seleccionado.
     * También automaticamente calcula la ruta desde la localización del móvil hacia la ruta. Por
     * defecto la calculará para ir andando.
     *
     * @param listaRutas
     */
    private void loadSpinner(final List<Ruta> listaRutas) {

        final ArrayList<String> rutasSpinner = new ArrayList<>();
        for (int i = 0; i < listaRutas.size(); i++) {
            rutasSpinner.add(listaRutas.get(i).getNombreRuta());
        }


        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, rutasSpinner));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mMap.clear();
                mMap.addPolyline(mapsUtil.drawPathFromLocalRoute(listaRutas.get(position).getPuntosRuta()));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(listaRutas.get(position).getPuntosRuta().get(0), 14));


                lSpinner = listaRutas;
                posicionSpinner = position;
                travelMode = "walking";

                String url = mapsUtil.getMapsApiDirectionsUrl(listaRutas.get(position), miPosicion, travelMode);
                webServiceObtenerRutaGoogle(url);
                addMarkers(miPosicion, listaRutas.get(position).getPuntosRuta().get(0));

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }


    /**
     * Método para realizar la petición hacia la API de Google para obtener la ruta entre dos puntos.
     * <p>
     * Si es correcto el resultado, será pintado en el mapa incluyendo la duración de la ruta gracias
     * a los métodos de MapUtils.
     *
     * @param url
     */
    private void webServiceObtenerRutaGoogle(String url) {

//        String url="https://maps.googleapis.com/maps/api/directions/json?origin="+latitudInicial+","+longitudInicial
//                +"&destination="+latitudFinal+","+longitudFinal;


        jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                List<List<HashMap<String, String>>> routes2;
                routes2 = mapsUtil.parseRoute(response);
                mMap.addPolyline(mapsUtil.drawPathFromGoogleResults(routes2, travelMode));
                infoRuta.setText(mapsUtil.getDuration());

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "No se puede conectar " + error.toString(), Toast.LENGTH_LONG).show();
                System.out.println();
                Log.d("ERROR: ", error.toString());
            }
        }
        );

        request.add(jsonObjectRequest);

    }


    /**
     * Método para añadir "marcas" en dos puntos.
     *
     * @param origen
     * @param destino
     */
    private void addMarkers(LatLng origen, LatLng destino) {
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions().position(origen));
            mMap.addMarker(new MarkerOptions().position(destino));
        }

    }

    /***Configuracion y envio de notificaciones***/

    /*
     * Método para la configuración del envío de las notificaciones Push para todas las versiones de Android.
     * */
    private void setUpNotification() {
        PackageManager pm = getApplicationContext().getPackageManager();
        Intent intent2 = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());

        intent2.setPackage(null);

        intent = PendingIntent.getActivity(this, 0, intent2, 0);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);

            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Método para enviar una notificación Push con la ruta indicada.
     *
     * @param rutas
     */
    private void sendNotification(List<String> rutas) {

        for (int i = 0; i < rutas.size(); i++) {

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "1")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(rutas.get(i))
                    .setContentIntent(intent)
                    .setPriority(3)
                    .setContentText("Esta ruta se encuentra a menos de " + (RANGE * 1000) + " metros!, comprueba el mapa.")
                    .setAutoCancel(true);

            notificationManager.notify((new Random().nextInt()), mBuilder.build());
        }
    }

}
