<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Pragma: no-cache');
header('X-Content-Type-Options: nosniff');

function fail_response(int $status, string $code, string $message): never {
    http_response_code($status);
    echo json_encode(['ok' => false, 'code' => $code, 'message' => $message], JSON_UNESCAPED_SLASHES);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    fail_response(405, 'method_not_allowed', 'POST required');
}

$secret = getenv('NUVEXA_PROVISIONING_SECRET');
if ($secret === false || strlen($secret) < 32) {
    fail_response(503, 'not_configured', 'Provisioning is not configured');
}

$raw = file_get_contents('php://input');
$data = json_decode($raw ?: '', true);
$installId = is_array($data) ? ($data['installId'] ?? '') : '';

if (!is_string($installId) || !preg_match('/^[A-Za-z0-9_-]{20,128}$/', $installId)) {
    fail_response(400, 'invalid_install_id', 'Invalid installation identifier');
}

// Stable, pseudonymous account identity. No credential or server secret is stored in the APK.
$userDigest = hash_hmac('sha256', 'user:' . $installId, $secret);
$username = 'nvx_' . substr($userDigest, 0, 24);
$passwordRaw = hash_hmac('sha256', 'password:' . $installId, $secret, true);
$password = rtrim(strtr(base64_encode($passwordRaw), '+/', '-_'), '=');

// Keep anonymous provisioning bounded on this managed test service.
$quota = getenv('NUVEXA_DEFAULT_QUOTA') ?: '1 GB';
$occ = 'php /var/www/html/occ';

exec($occ . ' user:info ' . escapeshellarg($username) . ' --output=json 2>&1', $infoOutput, $infoCode);

putenv('OC_PASS=' . $password);
if ($infoCode !== 0) {
    $createOutput = [];
    $createCode = 0;
    exec(
        'OC_PASS=' . escapeshellarg($password) . ' ' . $occ .
        ' user:add --password-from-env --display-name ' . escapeshellarg('Nuvexa User') . ' ' .
        escapeshellarg($username) . ' 2>&1',
        $createOutput,
        $createCode
    );

    if ($createCode !== 0) {
        fail_response(503, 'account_creation_failed', 'Could not provision a Nuvexa account');
    }
} else {
    // Idempotent retry: restore the deterministic device credential.
    $resetOutput = [];
    $resetCode = 0;
    exec(
        'OC_PASS=' . escapeshellarg($password) . ' ' . $occ .
        ' user:resetpassword --password-from-env ' . escapeshellarg($username) . ' 2>&1',
        $resetOutput,
        $resetCode
    );

    if ($resetCode !== 0) {
        fail_response(503, 'credential_refresh_failed', 'Could not refresh the Nuvexa credential');
    }
}

$quotaOutput = [];
$quotaCode = 0;
exec(
    $occ . ' user:setting ' . escapeshellarg($username) .
    ' files quota ' . escapeshellarg($quota) . ' 2>&1',
    $quotaOutput,
    $quotaCode
);

$host = $_SERVER['HTTP_X_FORWARDED_HOST'] ?? $_SERVER['HTTP_HOST'] ?? '';
$host = trim(explode(',', $host)[0]);
if ($host === '' || preg_match('/[^A-Za-z0-9.:-]/', $host)) {
    fail_response(500, 'invalid_host', 'Server host unavailable');
}

$scheme = $_SERVER['HTTP_X_FORWARDED_PROTO'] ?? 'https';
$scheme = strtolower(trim(explode(',', $scheme)[0])) === 'http' ? 'http' : 'https';

echo json_encode([
    'ok' => true,
    'server' => $scheme . '://' . $host,
    'loginName' => $username,
    'password' => $password,
    'quota' => $quota,
], JSON_UNESCAPED_SLASHES);
