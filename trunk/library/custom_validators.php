<?php
/**
 * custom validators
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

class Md5EqualValidator extends EqualValidator {
	/**
	 * @override
	 * @var string
	 */
	public $error = "Wrong password";

	/**
	 * @param string $comp
	 */
	public function __construct($comp) {
		parent::__construct($comp);
	}

	/**
	 * @param string $str
	 * @return bool
	 */
	public function isValid($str) {
		return parent::isValid(md5($str));
	}
}

class PriceValidator extends RegexValidator {
	/**
	 * @override
	 * @var string
	 */
	public $error = "This field is not a price";

	/**
	 * @override
	 */
	public function __construct() {
		parent::__construct('%^-{0,1}\d+.\d\d$%');
	}
}



