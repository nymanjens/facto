<?php

// start session
if(DEBUG)
    session_start();
else
    @session_start();

if (!isset($_SESSION["ip"])){
	// create session
	$_SESSION["ip"]=$_SERVER["REMOTE_ADDR"];
}

if ($_SERVER["REMOTE_ADDR"] != $_SESSION["ip"]){
	// Session Hijacking, unset session
	$_COOKIE["PHPSESSID"]="";
	session_unset();
	header("Location: ". $_SERVER["REQUEST_URI"]);
	exit();
}

