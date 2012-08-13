<?php
/**
 * i18n functionality
 *
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/**
 * Language class
 * 
 * provides very basic i18n
 */
class Language {
	
	/**
	 * constructor
	 * get locale file
	 */
	public function __construct($lang) {
		$locale_file = 'locale/lang_' . $lang . '.php';
		if(file_exists($locale_file))
			include_once $locale_file;
		else
			$dictionary = array();
		$this->dictionary = $dictionary;
	}
	
	/**
	 * translate function
	 * takes argument that replaces '%d' in the string, like printf
	 */
 	public function translate($string, $argument='') {
 		// translate
 		$strlower = strtolower($string);
 		if( isset($this->dictionary[$string]) ) {
 			$string = $this->dictionary[$string];
 		} else if( isset($this->dictionary[$strlower]) ) {
	 		$starts_with_upper = $string{0} === strtoupper($string{0}) && $string{1} !== strtoupper($string{1});
	 		$string = $this->dictionary[$strlower];
	 		if($starts_with_upper)
	 			$string = ucwords($string);
		}
		// parse argument
		if ($argument != '') {
			$string = sprintf($string, $argument);
		}
		return $string;
 	}
}

// global language instance
$lang = new Language(LANGUAGE);

// i18n utility function
function t($w, $arg=''){
	global $lang;
	return $lang->translate($w, $arg);
}



