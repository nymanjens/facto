<?php
/**
 * JForm default Validators
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

abstract class Validator {
	/**
	 * @var string
	 */
	public $error = "Invalid input";

	/**
	 * @param string $str
	 * @return bool
	 */
	public abstract function isValid($str);
}

class EqualValidator extends Validator {
	/**
	 * @override
	 * @var string
	 */
	public $error = "Not equal";

	/**
	 * @var string
	 */
	private $comp;

	/**
	 * @param string $comp
	 */
	public function __construct($comp) {
		$this->comp = $comp;
	}

	/**
	 * @param string $str
	 * @return bool
	 */
	public function isValid($str) {
		return $this->comp == $str;
	}
}

/**
 * 
 */
class ConstrainedSetValidator extends Validator {
	/**
	 * @var string
	 */
	private $options;

	/**
	 * @param array $options
	 */
	public function __construct($options) {
		// error check
		if(!is_array($options))
			die('Error: JForm->default_validators.php->ConstrainedSetValidator->constructor: $options is not an array');
		
		// shortcut for validating options of radiobuttons or dropdown menu
		if(is_array(reset($options)))
			$this->options = array_keys($options);
		else
			$this->options = $options;
	}

	/**
	 * @param string $str
	 * @return bool
	 */
	public function isValid($str) {
		return in_array($str, $this->options);
	}
}

class MaxLengthValidator extends Validator {
	/**
	 * @override
	 * @var string
	 */
	public $error = "Field can't be longer than <maxlen> characters";

	/**
	 * @var int
	 */
	private $maxlen;

	/**
	 * @param int $maxlen
	 */
	public function __construct($maxlen) {
		$this->maxlen = $maxlen;
		$this->error = str_replace("<maxlen>", $maxlen, $this->error);
	}

	/**
	 * @param string $str
	 * @return bool
	 */
	public function isValid($str) {
		return strlen($str) <= $this->maxlen;
	}
}

class PasswordValidator extends Validator {
	/**
	 * @var array
	 */
	private $errors = array(
		'invalid_chars' => "Password contains invalid characters",
		'minlen' => "Password must contain at least <minlen> characters",
		'maxlen' => "Password can't be longer than <maxlen> characters",
	);

	/**
	 * @override
	 * @var string
	 */
	public $error = "Invalid password";

	/**
	 * @var int
	 */
	private $minlen;

	/**
	 * @var int
	 */
	private $maxlen;

	/**
	 * @var array
	 */
	private $invalid_chars;

	/**
	 * @param int $minlen, int $maxlen
	 */
	public function __construct($minlen = 2, $maxlen = 20,
			$invalid_chars = array('\\', '"', '\'')) {
		$this->minlen = $minlen;
		$this->maxlen = $maxlen;
		$this->invalid_chars = $invalid_chars;
		$this->errors['minlen'] =
		str_replace("<minlen>", $minlen, $this->errors['minlen']);
		$this->errors['maxlen'] =
		str_replace("<maxlen>", $maxlen, $this->errors['maxlen']);
	}

	/**
	 * @param string $str
	 * @return bool
	 */
	public function isValid($str) {
		// check valid chars
		foreach($this->invalid_chars as $char) {
			if( strpos($str, $char) !== false ) {
				$this->error = $this->errors['invalid_chars'];
				return false;
			}
		}
		// check minlen
		if( strlen($str) < $this->minlen ) {
			$this->error = $this->errors['minlen'];
			return false;
		}
		// check maxlen
		if( strlen($str) > $this->maxlen ) {
			$this->error = $this->errors['maxlen'];
			return false;
		}
		return true;
	}
}

class RegexValidator extends Validator {
	/**
	 * @var string
	 */
	private $regex;

	/**
	 * @param string $regex
	 */
	public function __construct($regex) {
		$this->regex = $regex;
	}

	/**
	 * @param string $str
	 * @return bool
	 */
	public function isValid($str) {
		return preg_match($this->regex, $str);
	}
}

class NumericValidator extends RegexValidator {
	/**
	 * @override
	 * @var string
	 */
	public $error = "This field is not a number";

	/**
	 * @override
	 */
	public function __construct() {
		parent::__construct("/^[0-9]+$/");
	}
}

class EmailValidator extends RegexValidator {
	/**
	 * @override
	 * @var string
	 */
	public $error = "This is not an email address";

	/**
	 * @override
	 */
	public function __construct() {
		$pattern = '/^([a-z0-9])(([-a-z0-9._])*([a-z0-9]))*\@([a-z0-9])' .
			'(([a-z0-9-])*([a-z0-9]))+' .
			'(.([a-z0-9])([-a-z0-9_-])?([a-z0-9])+)+$/i';
		parent::__construct($pattern);
	}
}

/**
 * Checks if date is in the form of yyyy-mm-dd
 */
class DateValidator extends RegexValidator {
	/**
	 * @override
	 * @var string
	 */
	public $error = "Date should be in the format: yyyy-mm-dd";

	/**
	 * @override
	 */
	public function __construct() {
		parent::__construct('/^\d{2,4}-\d{1,2}-\d{1,2}$/');
	}

	/**
	 * @param string $str
	 * @return bool
	 */
	public function isValid($str) {
		if(parent::isValid($str))
		    return strtotime($str) !== FALSE;
		return false;
	}
}
