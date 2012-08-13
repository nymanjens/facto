<?php
/**
 * general functions for fc
 *
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/**
 * Debug: html-converted print_r
 *
 * @param mixed $s
 * @return void
 */
function print_pre($s){
	if(!function_exists('convertRecursively')){
	function convertRecursively($a){
		if(!is_array($a)) return htmlentities($a);
		foreach($a as &$elem)
			$elem = convertRecursively($elem);
		return $a;
	}}
	echo '<pre>';
	print_r(convertRecursively($s));
	echo '</pre>';
}

/**
 * Debug: print_r and die
 *
 * @param mixed $s
 * @return void
 */
function print_die($s){
	print_pre($s);
	die('');
}

/**
 * redirect the user and terminate all php-activity
 * 
 * @param string $destination
 * @return void
 */
function redirect($destination) {
	if(!@header("Location: ". $destination)) {
		if(DEBUG)
			die("<br /><br /><span style='color:red;'>Debugger:
				redirect to <a href='$destination'>
				\"$destination\"</a></span>");
		if(!preg_match("%^http%", $destination)) {
			// bad url, fix it
			function redirect_strleft($s1, $s2){
				return substr($s1, 0, strpos($s1, $s2));
			}
			$s = empty($_SERVER["HTTPS"]) ? ''
				: ($_SERVER["HTTPS"] == "on") ? "s"
				: "";
			$protocol = redirect_strleft(strtolower(
				$_SERVER["SERVER_PROTOCOL"]), "/").$s;
			$port = ($_SERVER["SERVER_PORT"] == "80") ? ""
				: (":".$_SERVER["SERVER_PORT"]);
			$destination = $protocol."://".$_SERVER['SERVER_NAME'].$port
				.$destination;
		}
		// is there's a #, it won't work
		if($pos = strpos($destination, '#') !== false)
			$destination = substr($destination, 0, $pos);
		die(sprintf('<script type="text/javascript">
			  window.location="%s";
			</script>', $destination));
	}
	exit;
}

/**
 * return the url of the page without the request (like /forum/...)
 * 
 * @return string
 */
function self_url_without_request(){
	static $url = "";
	if($url != "") return $url;
	if(!function_exists('self_url_without_request_strleft')){
	function self_url_without_request_strleft($s1, $s2){
		return substr($s1, 0, strpos($s1, $s2));
	}}
	$s = empty($_SERVER["HTTPS"]) ? ''
		: ($_SERVER["HTTPS"] == "on") ? "s"
		: "";
	$protocol = self_url_without_request_strleft(strtolower(
		$_SERVER["SERVER_PROTOCOL"]), "/").$s;
	$port = ($_SERVER["SERVER_PORT"] == "80") ? ""
		: (":".$_SERVER["SERVER_PORT"]);
	$url = $protocol."://".$_SERVER['SERVER_NAME'].$port;
	return $url;
}

/**
 * get requested url
 *
 * @return string
 */
function self_url(){
	$url = $_SERVER['REQUEST_URI'];
	$url = explode('#', $url);
	$url = $url[0];
	return $url;
}

/**
 * create new array with indexes starting from 0
 *
 * @param array $array
 * @return array
 */
function resetArrayIndexes(array $array) {
	$ret_array = array();
	foreach($array as $element)
	$ret_array[] = $element;
	return $ret_array;
}

/**
 * process some code in the default mysql database
 * 
 * @param string $code
 * @return mysql_result
 */
function query($code){
	$result = mysql_query($code) or die('<p>SQL-error: '
		.mysql_error().'</p><p>@ $q = <b>'.$code.'</b></p>');
	return($result);
}

/**
 * get first result variable
 * 
 * @param mysql_result $res
 * @return string
 */
function fetch_var($res){
	$array = mysql_fetch_array($res);
	return $array[0];
}

/**
 * get next id of table
 * 
 * @param string table
 * @return int
 */
function get_next_table_id($table) {
    $id = fetch_var(query("SELECT MAX(id) FROM ".TAG.$table));
    return $id ? $id + 1 : 1;
}

/**
 * returns encrypted email surrounded by an email link
 * 
 * @param string $email
 * @return string
 */
function JSEMAIL($email){
	$id = "LNK_".rand(0,10000000);
	$str = 	"var obj = document.getElementById('$id');".
			"obj.href = 'mailto:{$email}';".
			"obj.innerHTML = '$email';";
	$out = "";
	for($i=0; $i < strlen($str); $i++){
		$hex = dechex(ord($str[$i]));
		$out .= "%". ((strlen($hex) < 2) ? "0".$hex : $hex);
	}
	return "<a id='$id'>&nbsp;</a>
		<script type='text/javascript'>eval(unescape('$out'));</script>";
}

/**
 * Userfriendly time string
 * 
 * @param int $time
 * @return string
 */
function time_to_string($time){
	if(date('Y-m-d', $time) == date('Y-m-d') )
		return t("Today");
	if(date('z')-1 == date('z',$time) && date('Y') == date('Y',$time) )
		return t("Yesterday");
	if(time() - $time < 60*60*24*10)
		return sprintf('%s, %s %s', t(date("D", $time)), date("j", $time), t(date("M", $time)));
	if(date('y', $time) == date('y'))
		return sprintf('%s %s', date("j", $time), t(date("M", $time)));
	return sprintf('%s %s %s', date("j", $time), t(date("M", $time)), date("'y", $time));
}

/**
 * get file extension
 * 
 * @param string $path
 * @return string
 */
function file_get_extension($filepath) {
	preg_match('/[^?]*/', $filepath, $matches);
	$string = $matches[0];
	$pattern = preg_split('/\./', $string, -1, PREG_SPLIT_OFFSET_CAPTURE);
	// check if there is any extension
	if(count($pattern) == 1) {
		return "";
	}
	if(count($pattern) > 1) {
		$filenamepart = $pattern[count($pattern)-1][0];
		preg_match('/[^?]*/', $filenamepart, $matches);
		return $matches[0];
	}
}

/**
 * check if $number is numeric, only digits and a minus sign
 * is allowed
 * 
 * @param string $number
 * @return bool
 */
function numeric($number) {
	if($number == '')
		return false;
	if(is_int($number))
		return true;
	if(!is_numeric($number))
		return false;
	return preg_match("%^[-]{0,1}[0-9]+$%", $number) ? true : false;
}

/**
 * Userfriendly size string
 * 
 * @param int $bytes
 * @return string
 */
function file_bytes_to_str($bytes){
	//return $bytes.' B';
	$kB = ceil($bytes / 1000);
	if($kB > 1000)
		return round($kB/1000).' MB';
	else
		return $kB.' kB';
}

/**
 * cut off to $maxlen chars
 * if something is cut off, "..." and acronym is added
 * 
 * @param string $str
 * @param int $maxlen
 * @return string
 */
function cut_off_for_display($str, $maxlen) {
	if($maxlen < 4) $maxlen = 4;
	if(strlen($str) > $maxlen) {
		$short_str = substr($str, 0, $maxlen - 3);
		// heuristic: filter of half html entities
		// for example, &nbs..p;
		$short_str = preg_replace("%(&[a-z#0-9]{1,10})$%i", "", $short_str);
		return "<acronym title=\"$str\">$short_str...</acronym>";
	}
	return $str;
}

/**
 * get index of $element int $array
 * 
 * @param array $array
 * @param string $element
 * @return int
 */
function array_index_of(array $array, $element){
	for($i = 0; $i < sizeof($array); $i++)
		if($array[$i] == $element) return $i;
	return -1;
}
