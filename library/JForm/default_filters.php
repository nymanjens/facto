<?php
/**
 * JForm default Filters
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

abstract class Filter {
	/**
	 * @param string $str
	 * @return string
	 */
	public abstract function filter($str);
}

/**
 * overwrites value with given value
 */
class ValueSetFilter extends Filter {
	/**
	 * @var string
	 */
	private $value;
	
	/**
	 * @param string $value
	 */
	public function __construct($value) {
		$this->value = $value;
	}
	
	/**
	 * @param string $str
	 * @return string
	 */
	public function filter($str) {
		return $this->value;
	}
}

/**
 * cuts off more than $maxlen chars
 */
class CutOffFilter extends Filter {
	/**
	 * @var int
	 */
	private $maxlen;
	
	/**
	 * @param int $value
	 */
	public function __construct($maxlen) {
		$this->maxlen = $maxlen;
	}
	
	/**
	 * @param string $str
	 * @return string
	 */
	public function filter($str) {
		if(strlen($str) > $this->maxlen)
			return substr($str, 0, $this->maxlen);
		return $str;
	}
}

class DefaultTextFilter extends Filter {
	/**
	 * constructor
	 */
	public function __construct() {
		// do nothing
	}
	
	/**
	 * @param string $str
	 * @return string
	 */
	public function filter($str) {
		// trim string
		$str = trim($str);
		// get rid of backslashes
		$str = str_replace("\\", "&#92;", $str);
		// convert tags and quotes to html-values
		$str = htmlentities($str, ENT_QUOTES);
		return $str;
	}
}

class SqlPostFilter extends Filter {
	/**
	 * constructor
	 */
	public function __construct() {
		// do nothing
	}
	
	/**
	 * @param string $str
	 * @return string
	 */
	public function filter($str) {
		// sql safe
		if(function_exists('mysql_ping') && @mysql_ping())
    		$str = mysql_real_escape_string($str);
		else if(function_exists('pg_ping') && @pg_ping())
		    $str = pg_escape_string($str);
		else
		    $str = addslashes($str);
		return $str;
	}
}

class TextareaPostFilter extends Filter {
	/**
	 * constructor
	 */
	public function __construct() {
		// do nothing
	}
	
	/**
	 * @param string $str
	 * @return string
	 */
	public function filter($str) {
		// convert newlines to <br />
		$str = nl2br($str);
		// remove newlines
		$str = str_replace("\r", "", $str);
		$str = str_replace("\n", "", $str);
		return $str;
	}
}

class Md5PostFilter extends Filter {
	/**
	 * constructor
	 */
	public function __construct() {
		// do nothing
	}
	
	/**
	 * @param string $str
	 * @return string
	 */
	public function filter($str) {
		// sql safe
		$str = md5($str);
		return $str;
	}
}

class FileNameFilter extends Filter {
	/**
	 * @var array
	 */
	private static $ILLEGAL_CHARACTERS = array('/', '\\', '"',
		'<', '>', "'", ':', '*', '?', '|', '#');
	
	/**
	 * @override
	 */
	public function __construct() {
		// do nothing
	}
	
	/**
	 * @override
	 * @param string $str
	 * @return string
	 */
	public function filter($str) {
		foreach(self::$ILLEGAL_CHARACTERS as $char)
			$str = str_replace($char, '', $str);
		return $str;
	}
}

// example
//class [NAME]Filter extends Filter {
	/**
	 * @param string $str
	 * @return string
	 */
/*	public function filter($str) {
		return $str;
	}
}*/
