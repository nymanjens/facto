<?php
/**
 * user - list
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

$res = query("SELECT * FROM ".TAG."logs ORDER BY timestamp DESC LIMIT 200");
$logs = array();
while($array = mysql_fetch_array($res)) {
	$array['time'] = time_to_string($array['timestamp']);
	$array['user'] = get_username_from_id($array['user_id']);
	$array['sql'] = htmlentities($array['sql'], ENT_QUOTES);
	$logs[] = $array;
}

// output
$view->assign('logs', $logs);
$template = get_template("logs");
$dwoo->output($template, $view);
