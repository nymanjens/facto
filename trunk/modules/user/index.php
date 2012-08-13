<?php
/**
 * user controller
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// includes
//include dirname(__FILE__) . "/functions.inc.php";

// include running script
if(!isset($HTTP_REQUEST[1]))
	$HTTP_REQUEST[1] = '';
switch($HTTP_REQUEST[1]) {
case 'login':
	include dirname(__FILE__) . "/login.php";
	break;
case 'register':
	include dirname(__FILE__) . "/register.php";
	break;
case 'logout':
	include dirname(__FILE__) . "/logout.php";
	break;
case 'dashboard':
	include dirname(__FILE__) . "/dashboard.php";
	break;
case 'list':
case 'users':
	include dirname(__FILE__) . "/list.php";
	break;
default:
	if($user->isLoggedIn()) 
		include dirname(__FILE__) . "/dashboard.php";
	else
		include dirname(__FILE__) . "/login.php";
}

