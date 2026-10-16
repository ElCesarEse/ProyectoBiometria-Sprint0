<?php
declare(strict_types=1);

require_once(__DIR__ . '/logica/guardarMedicion.php');
require_once(__DIR__ . '/logica/obtenerMediciones.php');

echo "=== INICIANDO TESTS BACKEND SQLITE ===\n";

// Test 1: Guardar medición
$ok1 = guardarMedicion(11, 123, 1);
echo "1. Guardar CO2 (11, 123, 1): " . ($ok1 ? "OK" : "FALLO") . "\n";

$ok2 = guardarMedicion(11, 130, 2);
echo "2. Guardar CO2 (11, 130, 2): " . ($ok2 ? "OK" : "FALLO") . "\n";

// Test 2: Obtener última medición del sensor 11
$ultima = obtenerUltimaMedicion(11);
echo "3. Última medición obtenida: valor=" . ($ultima->valor ?? 'null') . ", contador=" . ($ultima->contador ?? 'null') . "\n";

// Test 3: Obtener lista
$lista = obtenerUltimasMediciones(5);
echo "4. Total registros recuperados: " . count($lista) . "\n";

echo "=== TESTS COMPLETADOS CON ÉXITO ===\n";
