<?php
/**
 * user controller
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// for link highlight
$view->assign('selected_link', 'user/users');

// includes
include dirname(__FILE__) . "/functions.inc.php";

// include running script
if(!isset($HTTP_REQUEST[1]))
	$HTTP_REQUEST[1] = '';
switch($HTTP_REQUEST[1]) {
case 'export':
	include dirname(__FILE__) . "/export.php";
	break;
case 'logs':
default:
	include dirname(__FILE__) . "/logs.php";
	break;
}

