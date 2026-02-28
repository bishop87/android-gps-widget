<?php
/**
 * GPS Tracker API - map_data2.php
 *
 * Restituisce le posizioni GPS filtrate per device e intervallo temporale.
 *
 * Parametri GET (tutti opzionali):
 *   device      - nome del device (stringa); se omesso restituisce tutti i device
 *   date_from   - data/ora inizio nel formato "Y-m-d H:i" (es. 2026-01-01 00:00)
 *   date_to     - data/ora fine   nel formato "Y-m-d H:i" (es. 2026-02-28 23:59)
 *
 * Risposta JSON (200 OK):
 *   {
 *     "success": true,
 *     "count": <int>,
 *     "filters": { "device": "...", "date_from": "...", "date_to": "..." },
 *     "data": [
 *       {
 *         "device":    "...",
 *         "timestamp": "...",
 *         "latitude":  "...",
 *         "longitude": "...",
 *         "accuracy":  "...",
 *         "battery":   "..."
 *       }, ...
 *     ]
 *   }
 *
 * Risposta JSON (errore):
 *   { "success": false, "error": "messaggio" }
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Risponde subito alle preflight OPTIONS (CORS)
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// Solo GET ammesso
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success' => false, 'error' => 'Method not allowed. Use GET.']);
    exit;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function apiError(int $httpCode, string $message): void
{
    http_response_code($httpCode);
    echo json_encode(['success' => false, 'error' => $message], JSON_UNESCAPED_UNICODE);
    exit;
}

function isValidDatetime(string $dt): bool
{
    if (empty($dt))
        return false;
    $d = DateTime::createFromFormat('Y-m-d H:i', $dt);
    return $d !== false && $d->format('Y-m-d H:i') === $dt;
}

// ---------------------------------------------------------------------------
// Lettura e validazione parametri
// ---------------------------------------------------------------------------

$device = isset($_GET['device']) ? trim($_GET['device']) : '';
$dateFrom = isset($_GET['date_from']) ? trim($_GET['date_from']) : '';
$dateTo = isset($_GET['date_to']) ? trim($_GET['date_to']) : '';

if (!empty($dateFrom) && !isValidDatetime($dateFrom)) {
    apiError(400, 'Parametro date_from non valido. Formato atteso: Y-m-d H:i (es. 2026-01-01 00:00)');
}

if (!empty($dateTo) && !isValidDatetime($dateTo)) {
    apiError(400, 'Parametro date_to non valido. Formato atteso: Y-m-d H:i (es. 2026-12-31 23:59)');
}

if (!empty($dateFrom) && !empty($dateTo) && $dateFrom > $dateTo) {
    apiError(400, 'date_from non può essere successivo a date_to');
}

// ---------------------------------------------------------------------------
// Connessione DB
// ---------------------------------------------------------------------------

require_once 'config.php';
$mysqli = getDbConnection();

if (!$mysqli) {
    apiError(503, 'Connessione al database non riuscita');
}

// ---------------------------------------------------------------------------
// Costruzione query con prepared statement
// ---------------------------------------------------------------------------

$conditions = [];
$types = '';
$params = [];

if (!empty($device)) {
    $conditions[] = 'device = ?';
    $types .= 's';
    $params[] = $device;
}

if (!empty($dateFrom)) {
    $conditions[] = 'timestamp >= ?';
    $types .= 's';
    $params[] = $dateFrom . ':00';   // aggiunge i secondi per il confronto DATETIME
}

if (!empty($dateTo)) {
    $conditions[] = 'timestamp <= ?';
    $types .= 's';
    $params[] = $dateTo . ':59';
}

$sql = 'SELECT device, timestamp, latitude, longitude, accuracy, battery FROM gps';
if (!empty($conditions)) {
    $sql .= ' WHERE ' . implode(' AND ', $conditions);
}
$sql .= ' ORDER BY timestamp DESC';

$stmt = $mysqli->prepare($sql);
if (!$stmt) {
    apiError(500, 'Errore nella preparazione della query: ' . $mysqli->error);
}

if (!empty($params)) {
    $stmt->bind_param($types, ...$params);
}

if (!$stmt->execute()) {
    apiError(500, 'Errore nell\'esecuzione della query: ' . $stmt->error);
}

$result = $stmt->get_result();

// ---------------------------------------------------------------------------
// Raccolta risultati
// ---------------------------------------------------------------------------

$data = [];
while ($row = $result->fetch_assoc()) {
    $data[] = [
        'device' => $row['device'],
        'timestamp' => $row['timestamp'],
        'latitude' => $row['latitude'],
        'longitude' => $row['longitude'],
        'accuracy' => $row['accuracy'],
        'battery' => $row['battery'],
    ];
}

$stmt->close();
$mysqli->close();

// ---------------------------------------------------------------------------
// Risposta
// ---------------------------------------------------------------------------

echo json_encode([
    'success' => true,
    'count' => count($data),
    'filters' => [
        'device' => $device ?: null,
        'date_from' => $dateFrom ?: null,
        'date_to' => $dateTo ?: null,
    ],
    'data' => $data,
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
?>