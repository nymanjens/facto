<?php
/**
 * local settings
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/*************** GENERAL SETTINGS ****************/
//define('ROOT_URL', "/"); // if ROOT_URL isn't defined, it's generated automatically
define('DEBUG', true); // show more debug output upon possible errors
define('SECRET_KEY', ''); // used for backup scripts, leave empty if it isn't needed

/*************** DATABASE SETTINGS ****************/
$mysql_host =     'localhost';
$mysql_name =     'root';
$mysql_password = 'pw';
$mysql_database = 'facto';
define('TAG', 'facto_'); // (optional) tag preceding database tables

