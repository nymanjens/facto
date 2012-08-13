<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// includes
include dirname(__FILE__) . "/functions.inc.php";

// get instant message
if($user->hasInstantMessage('message'))
	$view->assign('instant_message', $user->getInstantMessage('message'));
$view->assign('CATEGORIES', $CATEGORIES);

// include running script
if(!isset($HTTP_REQUEST[1]))
	$HTTP_REQUEST[1] = '';
switch($HTTP_REQUEST[1]) {
case 'overview':
	include dirname(__FILE__) . "/overview.php";
	break;
case 'input':
	include dirname(__FILE__) . "/action_input.php";
	break;
case 'edit':
	include dirname(__FILE__) . "/action_edit.php";
	break;
case 'set_balance':
	include dirname(__FILE__) . "/action_set_balance.php";
	break;
case 'liquidate':
	include dirname(__FILE__) . "/action_liquidate.php";
	break;
case 'withdrawal':
	include dirname(__FILE__) . "/action_withdrawal.php";
	break;
case 'endowment':
	include dirname(__FILE__) . "/action_endowment.php";
	break;
default:
	include dirname(__FILE__) . "/overview.php";
}

