<?php
/**
 * user - list
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// for link highlight
$view->assign('selected_link', 'user/users');

// user must be logged in to view this board
if(!$user->isLoggedIn())
	redirect(ROOT_URL);

$res = query("SELECT * FROM ".TAG."users ORDER BY last_visit DESC");
$users = array();
while($array = mysql_fetch_array($res)) {
	$array['last_visit'] = time_to_string($array['last_visit']);
	$users[] = $array;
}

// output
$view->assign('users', $users);
$template = get_template("list");
$dwoo->output($template, $view);
