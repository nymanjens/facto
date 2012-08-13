<?php
/**
 * general settings
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/*** functional settings ***/
define('LANGUAGE', 'en'); // /locale/lang_xx.php has to exist
define('CURRENCY_SYMBOL', '&euro;'); // e.g. '&dollar;', '&pound;' or '&euro;'
date_default_timezone_set("Europe/Brussels"); // timezone, see http://php.net/manual/en/timezones.php
define('SITE_TITLE', 'Family Accounting Tool'); // page title that shows up in browser and bookmarks

/*** advanced settings ***/
define('MAX_NAME_SIZE', 26); // max length of user's name
define('MAX_LOGIN_NAME_SIZE', 40); // max length login name
define('COOKIE_DAYS_KEPT', 100); // after this number of days without acitivity, the user will be logged off
if(DEBUG) ini_set("display_errors", 1);
define('HOME_MODULE', 'accounting');

