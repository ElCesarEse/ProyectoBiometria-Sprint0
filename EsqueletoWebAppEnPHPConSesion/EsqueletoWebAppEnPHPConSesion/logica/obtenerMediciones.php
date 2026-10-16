<?php
// --------------------------------------------------------------------------------
// Jordi Bataller / Proyecto Biometría y Medio Ambiente
//
// limite: N --> obtenerUltimasMediciones() --> [ Medicion ]
// sensor: Z --> obtenerUltimaMedicion() --> Medicion
//
// Medicion = ( id: N, sensor: Z, valor: Z, contador: N, fecha_hora: Text )
// --------------------------------------------------------------------------------

declare(strict_types=1);

require_once(__DIR__ . '/conexion.php');

function obtenerUltimasMediciones(int $limite = 10): array {
    try {
        $pdo = obtenerConexion();

        $sql = "SELECT id, sensor, valor, contador, fecha_hora 
                FROM Mediciones 
                ORDER BY fecha_hora DESC, id DESC 
                LIMIT :limite";

        $stmt = $pdo->prepare($sql);
        $stmt->bindValue(':limite', $limite, PDO::PARAM_INT);
        $stmt->execute();

        $filas = $stmt->fetchAll();
        return $filas ?: [];
    } catch (Exception $e) {
        error_log("Error en obtenerUltimasMediciones: " . $e->getMessage());
        return [];
    }
}

function obtenerUltimaMedicion(int $sensor): ?stdClass {
    try {
        $pdo = obtenerConexion();

        $sql = "SELECT id, sensor, valor, contador, fecha_hora 
                FROM Mediciones 
                WHERE sensor = :sensor 
                ORDER BY fecha_hora DESC, id DESC 
                LIMIT 1";

        $stmt = $pdo->prepare($sql);
        $stmt->bindValue(':sensor', $sensor, PDO::PARAM_INT);
        $stmt->execute();

        $medicion = $stmt->fetch();
        return $medicion ?: null;
    } catch (Exception $e) {
        error_log("Error en obtenerUltimaMedicion: " . $e->getMessage());
        return null;
    }
}
