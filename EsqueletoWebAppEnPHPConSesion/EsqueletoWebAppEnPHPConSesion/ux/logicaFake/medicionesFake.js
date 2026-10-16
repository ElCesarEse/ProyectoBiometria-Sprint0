// --------------------------------------------------------------------------------
// Jordi Bataller / Proyecto Biometría y Medio Ambiente
//
// Módulo de Lógica Fake para el navegador web (Cliente REST / Stubs)
// --------------------------------------------------------------------------------

// --------------------------------------------------------------------------------
// limite: N, cb: Callback --> obtenerUltimasMediciones() -->
// --------------------------------------------------------------------------------
function obtenerUltimasMediciones(limite, cb) {
    var url = "../rest/obtenerMediciones.php?limite=" + encodeURIComponent(limite || 10);

    fetch(url)
        .then(function(respuesta) {
            if (!respuesta.ok) {
                throw new Error("Error HTTP " + respuesta.status + ": " + respuesta.statusText);
            }
            return respuesta.json();
        })
        .then(function(datos) {
            if (typeof cb === 'function') {
                cb(null, datos);
            }
        })
        .catch(function(error) {
            console.error("Error en obtenerUltimasMediciones:", error);
            if (typeof cb === 'function') {
                cb(error.message, null);
            }
        });
}

// --------------------------------------------------------------------------------
// sensor: Z, cb: Callback --> obtenerUltimaMedicion() -->
// --------------------------------------------------------------------------------
function obtenerUltimaMedicion(sensor, cb) {
    var url = "../rest/obtenerMediciones.php?sensor=" + encodeURIComponent(sensor) + "&limite=1";

    fetch(url)
        .then(function(respuesta) {
            if (!respuesta.ok) {
                throw new Error("Error HTTP " + respuesta.status + ": " + respuesta.statusText);
            }
            return respuesta.json();
        })
        .then(function(dato) {
            if (typeof cb === 'function') {
                cb(null, dato);
            }
        })
        .catch(function(error) {
            console.error("Error en obtenerUltimaMedicion:", error);
            if (typeof cb === 'function') {
                cb(error.message, null);
            }
        });
}

// --------------------------------------------------------------------------------
// sensor: Z, valor: Z, contador: N, cb: Callback --> guardarMedicionFake() -->
// (Función auxiliar para pruebas desde la web)
// --------------------------------------------------------------------------------
function guardarMedicionFake(sensor, valor, contador, cb) {
    var url = "../rest/guardarMedicion.php";

    fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            sensor: parseInt(sensor, 10),
            valor: parseInt(valor, 10),
            contador: parseInt(contador, 10)
        })
    })
    .then(function(respuesta) {
        return respuesta.json().then(function(datos) {
            if (!respuesta.ok) {
                throw new Error(datos.error || ("Error HTTP " + respuesta.status));
            }
            return datos;
        });
    })
    .then(function(resultado) {
        if (typeof cb === 'function') {
            cb(null, resultado);
        }
    })
    .catch(function(error) {
        console.error("Error en guardarMedicionFake:", error);
        if (typeof cb === 'function') {
            cb(error.message, null);
        }
    });
}
