<?php
// --------------------------------------------------------------------------------
// Conexión a Base de Datos SQLite mediante PDO
// biometria_db (archivo biometria.db)
// --------------------------------------------------------------------------------

declare(strict_types=1);

function obtenerConexion(): PDO {
    static $pdo = null;

    if ($pdo === null) {
        $directorioBD = __DIR__ . '/../bd';
        if (!is_dir($directorioBD)) {
            mkdir($directorioBD, 0777, true);
        }

        $rutaArchivoBD = $directorioBD . '/biometria.db';
        $dsn = "sqlite:" . $rutaArchivoBD;

        try {
            $pdo = new PDO($dsn, null, null, [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_OBJ,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ]);

            // Crear automáticamente la tabla de mediciones si no existe
            $sqlCrearTabla = "
                CREATE TABLE IF NOT EXISTS Mediciones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sensor INTEGER NOT NULL,
                    valor INTEGER NOT NULL,
                    contador INTEGER NOT NULL,
                    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                CREATE INDEX IF NOT EXISTS idx_sensor_fecha ON Mediciones (sensor, fecha_hora DESC);
            ";
            $pdo->exec($sqlCrearTabla);

        } catch (PDOException $e) {
            error_log("Error de conexión a BD SQLite: " . $e->getMessage());
            throw $e;
        }
    }

    return $pdo;
}
