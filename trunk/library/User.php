<?php
/**
 * User class
 * 
 * Behavior:
 *  - stores session values in cookies and php session
 *  - manages user login
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */
class User {
	/**
	 * @var int
	 */
	public $id = -1;
	
	/**
	 * real username (not loginname)
	 * 
	 * @var string
	 */
	public $name = "";
	
	/**
	 * @var array
	 */
	public $databaseRow = array();
	
	/**
	 * constructor
	 * checks login info
	 */
	public function __construct() {
		$login_name = mysql_real_escape_string($this->getSession('login_name'));
		$password = mysql_real_escape_string($this->getSession('password'));
		if($login_name != "" && $password != "") {
			$res = query("SELECT * FROM ".TAG."users WHERE
				login_name='$login_name' AND password='$password' LIMIT 1");
			if(mysql_num_rows($res) == 1){ // user is logged in
				$o = mysql_fetch_object($res);
				// set id and username
				$this->id = $o->id;
				$this->name = $o->name;
				// update last visit
				$this->setDb('last_visit', time());
			}
		}
	}
	
	/**
	 * get variable from session/cookies
	 * 
	 * @param string $name
	 * @return string
	 */
	public function getSession($name) {
		if(isset($_COOKIE[TAG.$name]) && $_COOKIE[TAG.$name] != '')
			return $_COOKIE[TAG.$name];
		else if(isset($_SESSION[TAG.$name]) && $_SESSION[TAG.$name] != '')
			return $_SESSION[TAG.$name];
		return "";
	}
	
	/**
	 * set variable in session/cookies
	 * 
	 * @param string $name, string $value
	 * @return void
	 */
	public function setSession($name, $value) {
		if($name == 'name')
			$this->name = $value;
		$time = time() + COOKIE_DAYS_KEPT * 24 * 60 * 60;
		setcookie(TAG.$name, $value, $time, ROOT_URL);
		$_SESSION[TAG.$name] = $value;
	}
	
	/**
	 * get instant message (removed on access)
	 * 
	 * @param string $name
	 * @return string
	 */
	public function getInstantMessage($name) {
		$sess_name = TAG.'INST_'.$name;
		if(isset($_SESSION[$sess_name]) && $_SESSION[$sess_name] != '') {
			$tmp = $_SESSION[$sess_name];
			unset($_SESSION[$sess_name]);
			return $tmp;
		}
		return "";
	}
	
	/**
	 * set instant message in $_SESSION
	 * 
	 * @param string $name, string $value
	 * @return void
	 */
	public function setInstantMessage($name, $value) {
		$sess_name = TAG.'INST_'.$name;
		$_SESSION[$sess_name] = $value;
	}
	
	/**
	 * has instant message
	 * 
	 * @param string $name
	 * @return bool
	 */
	public function hasInstantMessage($name) {
		$sess_name = TAG.'INST_'.$name;
		return (isset($_SESSION[$sess_name]) && $_SESSION[$sess_name] != '');
	}
	
	/**
	 * get variable from database
	 * 
	 * @param string $name
	 * @return string
	 */
	public function getDb($name) {
		if(!$this->isLoggedIn())
			return "";
		$row = $this->getDbRow();
		return isset($row[$name]) ? $row[$name] : ""; 
	}
	
	/**
	 * set variable in database
	 * 
	 * @param string $name, string $value
	 * @return void
	 */
	public function setDb($name, $value) {
		if(!$this->isLoggedIn())
			return;
		query(sprintf("UPDATE ".TAG."users SET %s='%s'
			WHERE id=$this->id",
            mysql_real_escape_string($name),
            mysql_real_escape_string($value)));
        $this->databaseRow = array();
	}
	
	/**
	 * returns associative array from function mysql_fetch_array()
	 * 
	 * @return array
	 */
	public function getDbRow() {
		if(sizeof($this->databaseRow) == 0)
			$this->databaseRow = mysql_fetch_array(query(
				"SELECT * FROM ".TAG."users WHERE id=$this->id"));
		return $this->databaseRow;
	}
	
	/**
	 * returns if user is logged in
	 * 
	 * @return bool
	 */
	public function isLoggedIn() {
		return $this->id != -1;
	}
	
}

