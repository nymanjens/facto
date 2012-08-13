<?php
/**
 * custom filters
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/**
 * Dateformat: yyyy-mm-dd
 */
class DateToTimestampPostFilter extends Filter {
	/**
	 * constructor
	 */
	public function __construct() {
		// do nothing
	}
	
	/**
	 * @override
	 * @param string $str
	 * @return integer
	 */
	public function filter($str) {
		return strtotime($str . ' ' . date('H:i:s'));
	}
}

class ExtractPayedWithWhatAndWhoPostFilter extends Filter {
	/**
	 * constructor
	 */
	public function __construct() {
		// do nothing
	}
	
	/**
	 * @override
	 * @param string $str
	 * @return integer
	 */
	public function filter($str) {
	    global $PAYED_WITH_WHAT;
		foreach(array_keys($PAYED_WITH_WHAT) as $what) {
		    if(strpos($str, $what) === 0)
		        break;
		}
		$whose = substr($str, strlen($what) + 1);
		return array($what, $whose);
	}
}

class PriceFilter extends Filter {
	/**
	 * constructor
	 */
	public function __construct() {
		// do nothing
	}
	
	/**
	 * @override
	 * @param string $str
	 * @return integer
	 */
	public function filter($str) {
	    $str = trim($str);
	    $str = str_replace(' ', '', $str);
	    $str = str_replace('+', '', $str);
	    $str = str_replace(',', '.', $str);
	    if(strpos($str, '.') === false)
	        $str .= '.00';
        if(preg_match('%^-{0,1}\.%', $str))
            $str = '0' . $str;
        if(preg_match('%\.\d$%', $str))
            $str .= '0';
        if(preg_match('%\.$%', $str))
            $str .= '00';
	    return $str;
	}
}


