<?php
/**
 * user - logout
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// user must be logged in to view this board
if(!$user->isLoggedIn())
	redirect(ROOT_URL);

$user->setSession('password', '');
redirect(ROOT_URL.'user/login');

