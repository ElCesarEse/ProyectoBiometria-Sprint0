<?php
// --------------------------------------------------------------------------------
// GET ../rest/obtenerMediciones.php?sensor=<Z>&limite=<N>
//
// ( sensor: Z, limite: N ) --> rest/obtenerMediciones.php --> [ Medicion ]
// --------------------------------------------------------------------------------

declare(strict_types=1);

header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(["error" => "Método no permitido. Se requiere GET."]);
    exit;
}

require_once(__DIR__ . '/../logica/obtenerMediciones.php');

$limite = isset($_GET['limite']) ? (int)$_GET['limite'] : 10;
$sensor = isset($_GET['sensor']) ? (int)$_GET['sensor'] : null;

if ($sensor !== null && $limite === 1) {
    // Caso de uso: última medición de un sensor concreto
    $medicion = obtenerUltimaMedicion($sensor);
    http_response_code(200);
    echo json_encode($medicion ?: []);
} else {
    // Caso de uso: listado histórico de mediciones
    $mediciones = obtenerUltimasMediciones($limite);
    http_response_code(200);
    echo json_encode($mediciones);
}
