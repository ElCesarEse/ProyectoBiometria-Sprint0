package org.jordi.prueba2025;

import android.bluetooth.le.ScanSettings;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;

import android.support.v4.content.ContextCompat;

import android.os.Bundle;
import android.os.Build;
import android.Manifest;
import android.content.SharedPreferences;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private static final String ETIQUETA_LOG = ">>>>";

    private static final int CODIGO_PETICION_PERMISOS = 11223344;
    private static final int FABRICANTE_APPLE = 0x004c;
    private static final String UUID_BEACON_PROYECTO = "EPSG-GTI-PROY-3A";
    private static final String PREFERENCIAS_REST = "preferencias_rest";
    private static final String CLAVE_SERVIDOR_REST = "servidor_url";

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private BluetoothLeScanner elEscanner;

    private ScanCallback callbackDelEscaneo = null;

    private LogicaFake laLogicaFake = new LogicaFake();
    private int ultimoContadorProcesado = -1;

    private void mostrarConfiguracionServidorREST() {
        EditText campoURL = new EditText(this);
        campoURL.setSingleLine(true);
        campoURL.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        campoURL.setText(this.laLogicaFake.getServidorURL());
        campoURL.setSelection(campoURL.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Servidor REST")
                .setMessage("Introduce la URL base, por ejemplo http://192.168.1.10:8080")
                .setView(campoURL)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String url = campoURL.getText().toString().trim();
                    while (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }

                    Uri uri = Uri.parse(url);
                    String scheme = uri.getScheme();
                    if (uri.getHost() == null
                            || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                            || (uri.getPath() != null && !uri.getPath().isEmpty())) {
                        Toast.makeText(this, "Introduce una URL base HTTP(S) válida.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    this.laLogicaFake.setServidorURL(url);
                    getSharedPreferences(PREFERENCIAS_REST, MODE_PRIVATE)
                            .edit()
                            .putString(CLAVE_SERVIDOR_REST, url)
                            .apply();
                    Toast.makeText(this, "Servidor REST guardado: " + url, Toast.LENGTH_LONG).show();
                    Log.i(ETIQUETA_LOG, "Servidor REST configurado: " + url);
                })
                .show();
    }

    public void botonConfigurarServidorRESTPulsado(View v) {
        mostrarConfiguracionServidorREST();
    }

    private String[] permisosBTLERequeridos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
        return new String[] { Manifest.permission.ACCESS_FINE_LOCATION };
    }

    private boolean tienePermisosBTLE() {
        for (String permiso : permisosBTLERequeridos()) {
            if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean tienePermisoEscaneoBTLE() {
        String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? Manifest.permission.BLUETOOTH_SCAN
                : Manifest.permission.ACCESS_FINE_LOCATION;
        return ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED;
    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empieza ");
        if (this.elEscanner == null) {
            Log.e(ETIQUETA_LOG, "No hay escáner BLE disponible; comprueba que Bluetooth está activado.");
            return;
        }
        if (!tienePermisoEscaneoBTLE()) {
            Log.e(ETIQUETA_LOG, "Falta el permiso para escanear dispositivos BLE.");
            return;
        }
        detenerBusquedaDispositivosBTLE();

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): instalamos scan callback ");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "Anuncio BLE recibido por el escaneo general.");
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "Resultados BLE agrupados: " + results.size());
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanFailed(), código=" + errorCode);

            }
        };

        Log.i(ETIQUETA_LOG, "Iniciando escaneo BLE general (sin filtro de nombre).");
        this.elEscanner.startScan(this.callbackDelEscaneo);
        Log.i(ETIQUETA_LOG, "Escaneo BLE iniciado; esperando anuncios...");
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        if (resultado.getScanRecord() == null) {
            Log.w(ETIQUETA_LOG, "El resultado de escaneo no contiene ScanRecord.");
            return;
        }
        byte[] manufacturerData = resultado.getScanRecord().getManufacturerSpecificData(FABRICANTE_APPLE);
        if (manufacturerData == null) {
            Log.d(ETIQUETA_LOG, "Anuncio BLE recibido sin datos de fabricante Apple (0x004c).");
            return;
        }
        if (manufacturerData.length != 23
                || manufacturerData[0] != 0x02
                || manufacturerData[1] != 0x15) {
            Log.d(ETIQUETA_LOG, "Anuncio Apple ignorado: no es un iBeacon del proyecto.");
            return;
        }

        byte[] uuid = Arrays.copyOfRange(manufacturerData, 2, 18);
        String uuidBeacon = Utilidades.bytesToString(uuid);
        if (!UUID_BEACON_PROYECTO.equals(uuidBeacon)) {
            Log.d(ETIQUETA_LOG, "iBeacon de otro UUID ignorado: " + uuidBeacon);
            return;
        }

        int major = ((manufacturerData[18] & 0xff) << 8) | (manufacturerData[19] & 0xff);
        int minorSinSigno = ((manufacturerData[20] & 0xff) << 8) | (manufacturerData[21] & 0xff);
        int minor = (short) minorSinSigno;
        int sensor = (major >> 8) & 0xff;
        int contador = major & 0xFF;
        int txPower = manufacturerData[22];

        Log.i(ETIQUETA_LOG, "iBeacon GTI recibido: nombre="
                + resultado.getScanRecord().getDeviceName()
                + ", RSSI=" + resultado.getRssi()
                + ", UUID=" + uuidBeacon
                + ", major=" + major
                + ", minor=" + minor
                + ", txPower=" + txPower);
        Log.d(ETIQUETA_LOG, "Carga fabricante (0x004c): "
                + Utilidades.bytesToHexString(manufacturerData));

        // Evitar enviar peticiones duplicadas para el mismo paquete transmitido
        if (contador != this.ultimoContadorProcesado) {
            this.ultimoContadorProcesado = contador;
            Log.d(ETIQUETA_LOG, ">> [Receptor BLE] Capturado: Sensor=" + sensor
                    + " | Valor=" + minor + " | Cont=" + contador);

            this.laLogicaFake.guardarMedicion(sensor, minor, contador, new LogicaFake.CallbackGuardar() {
                @Override
                public void callback(boolean exito, int codigoHttp, String respuesta) {
                    Log.d(ETIQUETA_LOG, ">> [LogicaFake REST] Respuesta: exito=" + exito + " (HTTP " + codigoHttp + ") -> " + respuesta);
                }
            });
        }

    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private void buscarNuestroBeacon() {
        Log.d(ETIQUETA_LOG, "buscarNuestroBeacon(): empieza");
        if (this.elEscanner == null) {
            Log.e(ETIQUETA_LOG, "No hay escáner BLE disponible; comprueba que Bluetooth está activado.");
            return;
        }
        if (!tienePermisoEscaneoBTLE()) {
            Log.e(ETIQUETA_LOG, "Falta el permiso para escanear dispositivos BLE.");
            return;
        }
        detenerBusquedaDispositivosBTLE();

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "Anuncio BLE recibido; comprobando si es el iBeacon del proyecto.");
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "Resultados BLE agrupados: " + results.size());
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(ETIQUETA_LOG, "Falló el escaneo BLE; código=" + errorCode);
            }
        };

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        byte[] firmaBeacon = new byte[18];
        firmaBeacon[0] = 0x02;
        firmaBeacon[1] = 0x15;
        System.arraycopy(
                UUID_BEACON_PROYECTO.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                0,
                firmaBeacon,
                2,
                UUID_BEACON_PROYECTO.length());
        byte[] mascara = new byte[firmaBeacon.length];
        Arrays.fill(mascara, (byte) 0xff);
        ScanFilter filtroBeacon = new ScanFilter.Builder()
                .setManufacturerData(FABRICANTE_APPLE, firmaBeacon, mascara)
                .build();

        Log.i(ETIQUETA_LOG, "Iniciando escaneo BLE filtrado por el iBeacon "
                + UUID_BEACON_PROYECTO + ".");
        this.elEscanner.startScan(
                Collections.singletonList(filtroBeacon),
                settings,
                this.callbackDelEscaneo);
        Log.i(ETIQUETA_LOG, "Escaneo BLE iniciado; esperando el iBeacon del proyecto...");
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): empieza");
        if (this.callbackDelEscaneo == null) {
            Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): termina");
            return;
        }

        if (!tienePermisoEscaneoBTLE()) {
            Log.e(ETIQUETA_LOG, "Falta el permiso para detener el escaneo BLE.");
            return;
        }

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): parando scan");
        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null;

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): termina");
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton buscar dispositivos BTLE Pulsado");
        this.buscarTodosLosDispositivosBTLE();
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton nuestro dispositivo BTLE Pulsado");
        //this.buscarEsteDispositivoBTLE( Utilidades.stringToUUID( "EPSG-GTI-PROY-3A" ) );

        // Nombre configurado en la placa Arduino (Publicador.h)
        this.buscarNuestroBeacon();

    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton detener busqueda dispositivos BTLE Pulsado");
        this.detenerBusquedaDispositivosBTLE();
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): empieza ");
        if (!tienePermisosBTLE()) {
            Log.i(ETIQUETA_LOG, "Faltan permisos BLE; mostrando solicitud al usuario.");
            requestPermissions(permisosBTLERequeridos(), CODIGO_PETICION_PERMISOS);
            return;
        }

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            Log.e(ETIQUETA_LOG, "Este dispositivo no dispone de Bluetooth.");
            return;
        }

        if (!bta.isEnabled()) {
            Log.w(ETIQUETA_LOG, "Bluetooth está desactivado; solicitando activación al usuario.");
            startActivityForResult(
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    CODIGO_PETICION_PERMISOS + 1);
            return;
        }

        this.elEscanner = bta.getBluetoothLeScanner();
        if (this.elEscanner == null) {
            Log.e(ETIQUETA_LOG, "Bluetooth está activado, pero no se pudo obtener el escáner BLE.");
        } else {
            Log.d(ETIQUETA_LOG, "Escáner BLE inicializado.");
        }
    } // ()


    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, " onCreate(): empieza ");

        String servidorGuardado = getSharedPreferences(PREFERENCIAS_REST, MODE_PRIVATE)
                .getString(CLAVE_SERVIDOR_REST, null);
        if (servidorGuardado != null) {
            this.laLogicaFake.setServidorURL(servidorGuardado);
        }
        Log.i(ETIQUETA_LOG, "Servidor REST actual: " + this.laLogicaFake.getServidorURL());

        inicializarBlueTooth();

        Log.d(ETIQUETA_LOG, " onCreate(): termina ");

    } // onCreate()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PETICION_PERMISOS) {
            if (grantResults.length > 0 && tienePermisosBTLE()) {
                Log.i(ETIQUETA_LOG, "Permisos BLE concedidos.");
                inicializarBlueTooth();
            } else {
                Log.e(ETIQUETA_LOG, "Permisos BLE denegados; no se puede iniciar el escaneo.");
            }
        }
    } // ()

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODIGO_PETICION_PERMISOS + 1) {
            if (resultCode == RESULT_OK) {
                inicializarBlueTooth();
            } else {
                Log.e(ETIQUETA_LOG, "El usuario no activó Bluetooth; el escaneo BLE no puede comenzar.");
            }
        }
    }

} // class