<?php
/**
 * site index
 * main controller, all sites must pass this script
 * 
 * this website only works when it's found at /
 * to establish that, a virtual host can be used
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// for included scripts
define("IN_INDEX", true);

/********************* includes *********************/
include_once 'library/dwoo/dwooAutoload.php'; //dwoo template engine
include_once 'library/JForm/autoload.php'; // JForm form engine
include_once 'library/custom_filters.php'; // JForm filters
include_once 'library/custom_validators.php'; // JForm validators
include_once 'library/DynamicOptionsForm.php'; // DynamicOptionsForm object
include_once 'library/CustomPriceForm.php'; // CustomPriceForm object
include_once 'library/functions_general.php'; // general functions
include_once 'library/functions_specific.php'; // specific functions
include_once 'settings_local.php'; // local settings
include_once 'library/sessions.php'; // starts session
include_once 'library/User.php'; // user object
include_once 'settings.php'; // supported settings
include_once 'library/i18n.php'; // user object
include_once 'settings_layout.php'; // layout settings
include_once 'settings_accounting.php'; // accounting settings

/*************** connect to database ****************/
mysql_connect($mysql_host, $mysql_name, $mysql_password)
	or die('Could not connect to MySQL database, the server returned the error: '.mysql_error());
mysql_select_db($mysql_database)
	or die('An error occured while selecting a database: '.mysql_error());

/**************** additional starts *****************/
// user
$user = new User();

/***************** request analysis *****************/
// get request without GET variables
$request_uri = reset(explode('?', $_SERVER['REQUEST_URI']));
// split the path by '/'
$http_request = explode("/", $request_uri);
for($i = sizeof($http_request) - 1; $i >= 0; $i--)
	if(trim($http_request[$i]) == "")
		unset($http_request[$i]);
$http_request = resetArrayIndexes($http_request);
// heuristic: remove uninteresting directory path
$folder_separator = (strpos(__FILE__, "\\") !== false)
	? "\\" : "/";
$directory_folders = explode($folder_separator, dirname(__FILE__));
for($i = 0; $i < min(sizeof($directory_folders),
		sizeof($http_request));	$i++) {
	$match = true;
	for($j = 0; $j <= $i; $j++) {
		if(urldecode($http_request[$j]) != $directory_folders[
				sizeof($directory_folders) - 1 - $i + $j]) {
			$match = false;
			break;
		}
	}
	if($match) {
		for($j = $i; $j >= 0; $j--)
			unset($http_request[$j]);
		$http_request = resetArrayIndexes($http_request);
		break;
	}
}
// define variable that will be used as argument
// in included scripts
$HTTP_REQUEST = $http_request;
// done with these variables
unset($http_request);
unset($directory_folders);

/******************* get module *********************/
// if nothing is set, use the home module
if(!isset($HTTP_REQUEST[0]) || $HTTP_REQUEST[0] == "")
	$HTTP_REQUEST[0] = HOME_MODULE;
// check for illegal characters
$ILLEGAL_CHARACTERS = array("\\", "/", ".");
foreach($ILLEGAL_CHARACTERS as $char)
	if(strpos($HTTP_REQUEST[0], $char) !== false)
		die("Fatal error: module contains illegal characters");
unset($ILLEGAL_CHARACTERS);
// check if module exists 
if(!file_exists('modules/' . $HTTP_REQUEST[0]))
	$HTTP_REQUEST[0] = HOME_MODULE;
// defione module
define('MODULE', $HTTP_REQUEST[0]);

/**************** heuristic: ROOT_URL ***************/
if(!defined('ROOT_URL')) {
	$pos = strpos($_SERVER['REQUEST_URI'], "/".MODULE);
	if($pos === false)
		$pos = strrpos($_SERVER['REQUEST_URI'], "/");
	if($pos === false)
		define('ROOT_URL', "/");
	else
		define('ROOT_URL', substr($_SERVER['REQUEST_URI'],
			0, $pos + 1));
}

/****************** restrict not-loggedin users to user module ********************/
if(!$user->isLoggedIn()) {
    if(MODULE == 'user')
    	; // this module is public
	else if(MODULE == 'admin' && isset($_GET['SECRET_KEY']) && $_GET['SECRET_KEY'] ==  SECRET_KEY && SECRET_KEY ) {
		; // secret key equals logged in by admin
	} else
        redirect(ROOT_URL.'user/');
}

/****************** set template ********************/
$dwoo = new Dwoo();
$view = new Dwoo_Data();
$view->assign('page_title', SITE_TITLE);
$view->assign('currency_symbol', CURRENCY_SYMBOL);
// set name in label
if(!$user->isLoggedIn()) {
	$LAYOUT_TOP_LINKS = $LAYOUT_TOP_LINKS_LOGGEDOUT;
} else {
	$LAYOUT_TOP_LINKS = $LAYOUT_TOP_LINKS_LOGGEDIN;
	foreach($LAYOUT_TOP_LINKS as &$elem)
		$elem['label'] = str_replace("<name>", $user->name,
			$elem['label']);
}
unset($elem);

$view->assign('top_links', $LAYOUT_TOP_LINKS);
$view->assign('admin_email', JSEMAIL('nymanjens.nj@gmail.com'));
$view->assign('root_url', ROOT_URL);
if(file_exists('scripts/' . MODULE . '.css'))
	$view->assign('include_style', array('scripts/' . MODULE . '.css'));
if(file_exists('scripts/' . MODULE . '.js'))
	$view->assign('include_script', array('scripts/' . MODULE . '.js'));
$view->assign('self_url', self_url());

/****************** apply request *******************/
// module controller is located in index
include 'modules/' . MODULE . '/index.php';


