package org.jordi.prueba2025;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// --------------------------------------------------------------------------------
// Jordi Bataller / Proyecto Biometría y Medio Ambiente
//
// Clase Proxy / Lógica Fake para la aplicación Android
// Encapsula las llamadas REST asíncronas hacia el servidor backend
// --------------------------------------------------------------------------------
public class LogicaFake {

    private static final String ETIQUETA_LOG = "LogicaFake";

    // IP local de tu PC en la Wi-Fi (para que el móvil físico pueda conectarse por la misma red)
    // Si usas el emulador de Android Studio en el propio PC, usarías "http://10.0.2.2:8080"
    private String servidorURL = "http://10.0.2.2:8080";

    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());

    // --------------------------------------------------------------------------------
    // Interfaz de Callback asíncrono
    // exito: B, codigo: Z --> callback() -->
    // --------------------------------------------------------------------------------
    public interface CallbackGuardar {
        void callback(boolean exito, int codigoHttp, String respuesta);
    }

    // --------------------------------------------------------------------------------
    // Constructor
    // --------------------------------------------------------------------------------
    public LogicaFake() {
    }

    public LogicaFake(String servidorURL) {
        this.servidorURL = servidorURL;
    }

    public void setServidorURL(String url) {
        this.servidorURL = url;
    }

    public String getServidorURL() {
        return this.servidorURL;
    }

    // --------------------------------------------------------------------------------
    // sensor: Z, valor: Z, contador: N, cb: CallbackGuardar --> guardarMedicion() -->
    // --------------------------------------------------------------------------------
    public void guardarMedicion(final int sensor, final int valor, final int contador, final CallbackGuardar callback) {
        Log.d(ETIQUETA_LOG, "guardarMedicion(): preparando envío -> sensor=" + sensor + " valor=" + valor + " contador=" + contador);

        ejecutor.execute(new Runnable() {
            @Override
            public void run() {
                int codigoRespuesta = 0;
                String cuerpoRespuesta = "";
                boolean exito = false;

                HttpURLConnection conexion = null;
                try {
                    String urlDestino = servidorURL + "/rest/guardarMedicion.php";
                    Log.d(ETIQUETA_LOG, "Conectando a: " + urlDestino);

                    URL url = new URL(urlDestino);
                    conexion = (HttpURLConnection) url.openConnection();
                    conexion.setRequestMethod("POST");
                    conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conexion.setRequestProperty("Accept", "application/json");
                    conexion.setConnectTimeout(5000);
                    conexion.setReadTimeout(5000);
                    conexion.setDoOutput(true);
                    conexion.setDoInput(true);

                    // Construcción del JSON
                    JSONObject json = new JSONObject();
                    json.put("sensor", sensor);
                    json.put("valor", valor);
                    json.put("contador", contador);
                    String jsonString = json.toString();

                    Log.d(ETIQUETA_LOG, "Payload JSON enviado: " + jsonString);

                    // Escritura segura en UTF-8
                    OutputStream os = conexion.getOutputStream();
                    os.write(jsonString.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();

                    codigoRespuesta = conexion.getResponseCode();
                    Log.d(ETIQUETA_LOG, "Código respuesta HTTP: " + codigoRespuesta);

                    InputStream is;
                    if (codigoRespuesta >= 200 && codigoRespuesta < 300) {
                        is = conexion.getInputStream();
                        exito = true;
                    } else {
                        is = conexion.getErrorStream();
                    }

                    if (is != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String linea;
                        while ((linea = br.readLine()) != null) {
                            sb.append(linea);
                        }
                        br.close();
                        cuerpoRespuesta = sb.toString();
                    }

                    Log.d(ETIQUETA_LOG, "Cuerpo respuesta: " + cuerpoRespuesta);

                } catch (Exception e) {
                    Log.e(ETIQUETA_LOG, "Error en petición REST: " + e.getMessage(), e);
                    cuerpoRespuesta = "Excepción: " + e.getMessage();
                    exito = false;
                } finally {
                    if (conexion != null) {
                        conexion.disconnect();
                    }
                }

                // Notificar en el hilo de la UI si se facilitó callback
                if (callback != null) {
                    final boolean exitoFinal = exito;
                    final int codigoFinal = codigoRespuesta;
                    final String respuestaFinal = cuerpoRespuesta;

                    handlerPrincipal.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.callback(exitoFinal, codigoFinal, respuestaFinal);
                        }
                    });
                }
            }
        });
    }
}
