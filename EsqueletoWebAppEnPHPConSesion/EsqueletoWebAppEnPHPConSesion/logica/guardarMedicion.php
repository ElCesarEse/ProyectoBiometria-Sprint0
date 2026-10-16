<?php
// --------------------------------------------------------------------------------
// Jordi Bataller / Proyecto Biometría y Medio Ambiente
//
// sensor: Z, valor: Z, contador: N --> guardarMedicion() --> B
//
// Guarda una nueva medición en la base de datos relacional SQLite
// --------------------------------------------------------------------------------

declare(strict_types=1);

require_once(__DIR__ . '/conexion.php');

function guardarMedicion(int $sensor, int $valor, int $contador): bool {
    try {
        $pdo = obtenerConexion();

        $sql = "INSERT INTO Mediciones (sensor, valor, contador, fecha_hora) 
                VALUES (:sensor, :valor, :contador, datetime('now', 'localtime'))";
        
        $stmt = $pdo->prepare($sql);
        $resultado = $stmt->execute([
            ':sensor'   => $sensor,
            ':valor'    => $valor,
            ':contador' => $contador
        ]);

        return $resultado;
    } catch (Exception $e) {
        error_log("Error en guardarMedicion: " . $e->getMessage());
        return false;
    }
}
