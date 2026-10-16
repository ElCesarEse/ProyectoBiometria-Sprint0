<?php
// --------------------------------------------------------------------------------
// POST ../rest/guardarMedicion.php
//
// ( sensor: Z, valor: Z, contador: N ) --> rest/guardarMedicion.php --> ( resultado: B )
// --------------------------------------------------------------------------------

declare(strict_types=1);

header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Gestión de preflight CORS (OPTIONS)
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["error" => "Método no permitido. Se requiere POST."]);
    exit;
}

require_once(__DIR__ . '/../logica/guardarMedicion.php');

// Intentar leer el cuerpo crudo de la petición (JSON)
$cuerpoJSON = file_get_contents('php://input');
$datos = json_decode($cuerpoJSON, true);

// Si no venía en JSON, comprobar si vino por application/x-www-form-urlencoded
if (!is_array($datos)) {
    $datos = $_POST;
}

// Validar presencia de los 3 campos obligatorios
if (!isset($datos['sensor']) || !isset($datos['valor']) || !isset($datos['contador'])) {
    http_response_code(400);
    echo json_encode(["error" => "Parámetros incompletos. Se requieren 'sensor', 'valor' y 'contador'."]);
    exit;
}

$sensor = (int)$datos['sensor'];
$valor = (int)$datos['valor'];
$contador = (int)$datos['contador'];

$exito = guardarMedicion($sensor, $valor, $contador);

if ($exito) {
    http_response_code(201);
    echo json_encode([
        "resultado" => true,
        "mensaje"   => "Medición guardada correctamente",
        "sensor"    => $sensor,
        "valor"     => $valor,
        "contador"  => $contador
    ]);
} else {
    http_response_code(500);
    echo json_encode(["resultado" => false, "error" => "No se pudo guardar la medición en la base de datos"]);
}
