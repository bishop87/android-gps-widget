<?php
/**
 * Configurazione Database MySQL
 * Questo file contiene le credenziali di accesso al database.
 * NON committare questo file nel repository!
 */

define('DB_HOST', 'localhost');
define('DB_USER', 'username');
define('DB_PASS', 'password');
define('DB_NAME', 'dbname');

/**
 * Funzione helper per la connessione al database
 */
sfunction getDbConnection()
{
    $mysqli = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
    if ($mysqli->connect_error) {
        return null;
    }
    return $mysqli;
}
?>
