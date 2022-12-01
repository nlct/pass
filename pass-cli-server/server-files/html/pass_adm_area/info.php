<?php
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Server Info');
$pass->require_admin();// ensure user is logged in as admin

ob_start();
phpinfo();
$html = ob_get_contents();
ob_end_clean();

$html = str_replace('<body>', '<body><a href="index.php">Admin Home</a>', $html);
$html = str_replace('</body>', '<a href="index.php">Admin Home</a></body>', $html);

if (isset($_SERVER['AUTH_USER'])) $html = str_replace($_SERVER['AUTH_USER'], '[***]', $html);
if (isset($_SERVER['AUTH_PASSWORD'])) $html = str_replace($_SERVER['AUTH_PASSWORD'], '[***]', $html);
if (isset($_SERVER['PHP_AUTH_PW'])) $html = str_replace($_SERVER['PHP_AUTH_PW'], '[***]', $html);

echo $html;
?>
