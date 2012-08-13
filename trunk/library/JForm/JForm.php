<?php
/**
 * JForm form engine
 *
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 * @version 1.0 beta
 * 
 * Copyright (c) 2010, Jens Nyman <nymanjens.nj@gmail.com>
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the owner nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

class JForm {
	/**
	 * @var array
	 */
	public $data;

	/**
	 * ignored attributes in lowercase
	 * 
	 * @var array
	 */
	protected static $IGNORED_ATTRIB = array('error', 'label', 'inner_html',
		'filters', 'postfilters', 'validators', 'tag', 'required');

	/**
	 * @var int
	 */
	protected static $formNameGenCounter = 0;

	/**
	 * @param array $data
	 */
	public function __construct(array $data) {
		// check/generate name
		if(!isset($data['name']) || $data['name'] == "")
			$data['name'] = "f_" . self::$formNameGenCounter++;
		// generate submit
		if(!isset($data['submit_name']))
			$data['submit_name'] = 'submit_'.$data['name'];
		if(!isset($data['inputs'][$data['submit_name']]))
			$data['inputs'][$data['submit_name']] = array(
				'type' => "submit",
				'value' => isset($data['submit_label'])?
					$data['submit_label']: "Ok",
			);
		// check anchor
		if(isset($data['anchor'])) {
			if($data['anchor'] == "")
				$data['anchor'] = $data['name'];
			if(!isset($data['url']) || $data['url'] == "")
				$data['url'] = jform_self_url() . "#" . $data['anchor'];
		}
		if(!isset($data['url']) || $data['url'] == "")
			$data['url'] = jform_self_url();
		// anti bot key
		if(!isset($data['inputs']['anti_bot_key'])) {
			$anti_bot_key = md5(implode(array_keys($data['inputs'])));
			$data['inputs']['anti_bot_key'] = array(
				'type' => "hidden",
				'required' => "true",
				'value' => $anti_bot_key,
				'validators' => new EqualValidator($anti_bot_key),
			);
		}
		// check inputs
		foreach($data['inputs'] as $name => &$attrib){
			// check filters
			if(!isset($attrib['filters']) || $attrib['filters'] == "")
				$attrib['filters'] = array();
			else if( !is_array($attrib['filters']) )
				$attrib['filters'] = array( $attrib['filters'] );
			// check postfilters
			if(!isset($attrib['postfilters']) || $attrib['postfilters'] == "")
				$attrib['postfilters'] = array();
			else if( !is_array($attrib['postfilters']) )
				$attrib['postfilters'] = array( $attrib['postfilters'] );
			// check validators
			if(!isset($attrib['validators']) || $attrib['validators'] == "")
				$attrib['validators'] = array();
			else if( !is_array($attrib['validators']) )
				$attrib['validators'] = array( $attrib['validators'] );
			// set name attrib
			// if(!isset($attrib['name']) || $attrib['name'] == "")
			$attrib['name'] = $name;
			// check type
			if(!isset($attrib['type']) || $attrib['type'] == "")
				$attrib['type'] = 'text';
			// chack value
			if(!isset($attrib['value']))
				$attrib['value'] = '';
			// check tag
			if(!isset($attrib['tag']) || $attrib['tag'] == "")
				$attrib['tag'] = 'input';
			// check label
			if(!isset($attrib['label']))
				$attrib['label'] = self::formatString($name);
			// check required
			if(isset($attrib['required']) && $attrib['required'])
				$attrib['required'] = true;
			else
				$attrib['required'] = false;
			// check maxlength
			if(isset($attrib['maxlength']) &&
					is_numeric($attrib['maxlength']))
				array_unshift($attrib['filters'], new
					CutOffFilter($attrib['maxlength'])); // filter at start
			// special case: hidden value
			if(strtolower($attrib['type']) == 'hidden') {
				if(sizeof($attrib['filters']) == 0 &&
						sizeof($attrib['validators']) == 0) {
					$attrib['filters'][] = new ValueSetFilter($attrib['value']);
					if($attrib['value'] != "")
						$attrib['required'] = true;
				}
			}
			// special case: static value
			if(strtolower($attrib['type']) == 'static') {
				$attrib['filters'][] = new ValueSetFilter($attrib['value']);
				if($attrib['value'] != "")
					$attrib['required'] = true;
				$attrib['disabled'] = "disabled";
			}
			// special case: radio
			if(strtolower($attrib['type']) == 'radio') {
				if(!isset($attrib['html-left']))
					$attrib['html-left'] = "";
				if(!isset($attrib['html-center']))
					$attrib['html-center'] = " ";
				if(!isset($attrib['html-right']))
					$attrib['html-right'] = "<br />";
				$attrib['validators'][] = new ConstrainedSetValidator(
					$attrib['options']);
			}
			// special case: select (dropdown menu)
			if(strtolower($attrib['type']) == 'select') {
				$attrib['validators'][] = new ConstrainedSetValidator(
					$attrib['options']);
			}
			// special case: checkbox
			if(strtolower($attrib['type']) == 'checkbox') {
				// value means returned string if checked
				if($attrib['value'] == "")
					$attrib['value'] = 1;
			}
		}
		$this->data = $data;
	}

	/**
	 * from computer-friendly to human-fiendly format
	 *
	 * @param string $s
	 * @return string
	 */
	protected static function formatString($s) {
		$s = str_replace('_', ' ', $s);
		$s = ucwords($s);
		return $s;
	}

	/**
	 * @return bool
	 */
	public function isSubmitted() {
		return isset($_POST[$this->data['submit_name']]) &&
		$_POST[$this->data['submit_name']] != "";
	}

	/**
	 * get template html code
	 *
	 * @return array
	 */
	public function getHtml() {
		$html = array();
		// set inputs
		$html['inputs'] = array();
		foreach($this->data['inputs'] as $name => &$attrib) {
			$ignore_list = self::$IGNORED_ATTRIB;
			$html['inputs'][$name] = array();
			// special case: textarea
			if(strtolower($attrib['type']) == 'textarea') {
				$ignore_list[] = 'type';
				$attrib['tag'] = 'textarea';
				$ignore_list[] = 'value';
				$attrib['inner_html'] = isset($attrib['value']) ?
					$attrib['value'] : "";
			}
			// set html
			// special case: radio
			if(strtolower($attrib['type']) == 'radio') {
				// set ignores
				$ignore_list[] = 'options';
				$ignore_list[] = 'html-left';
				$ignore_list[] = 'html-center';
				$ignore_list[] = 'html-right';
				// cycle trough options and append to $html_line
				$html_line = "";
				foreach($attrib['options'] as $opt_name => $opt_attrib) {
					if(!isset($opt_attrib['label']))
						$opt_attrib['label'] = self::formatString($opt_name);
					$opt_attrib = array_merge($attrib, $opt_attrib);
					$opt_attrib['value'] = $opt_name;
					if($opt_attrib['value'] == $attrib['value'])
						$opt_attrib['checked'] = 'checked';
					$html_line .= "{$opt_attrib['html-left']}"
						. "<{$attrib['tag']}";
					foreach($opt_attrib as $aname => $aval) {
						if( in_array(strtolower($aname), $ignore_list) )
							continue;
						$html_line .= " $aname='$aval'";
					}
					$html_line .= " />{$opt_attrib['html-center']}".
						"{$opt_attrib['label']}{$opt_attrib['html-right']}";
				}
				$html['inputs'][$name]['input'] = $html_line;
			
			// special case: select (dropdown menu)
			} else if(strtolower($attrib['type']) == 'select') {
				// set ignores
				$ignore_list[] = 'options';
				$html_line = "<select";
				foreach($attrib as $aname => $aval) {
					if( in_array(strtolower($aname), $ignore_list) 
							|| strtolower($aname) == 'value')
						continue;
					$html_line .= " $aname='$aval'";
				}
				$html_line .= " >";
				// cycle trough options and append to $html_line
				foreach($attrib['options'] as $opt_name => $opt_attrib) {
					if(!isset($opt_attrib['label']))
						$opt_attrib['label'] = self::formatString($opt_name);
					$opt_attrib['value'] = $opt_name;
					if($opt_attrib['value'] == $attrib['value'])
						$opt_attrib['selected'] = 'selected';
					$html_line .= "<option";
					foreach($opt_attrib as $aname => $aval) {
						if( in_array(strtolower($aname), $ignore_list) )
							continue;
						$html_line .= " $aname='$aval'";
					}
					$html_line .= " >{$opt_attrib['label']}</option>";
				}
				$html_line .= '</select>';
				$html['inputs'][$name]['input'] = $html_line;
				
			} else {
				$html_line = "<{$attrib['tag']}";
				foreach($attrib as $aname => $aval) {
					if( in_array(strtolower($aname), $ignore_list) )
						continue;
					$html_line .= " $aname='$aval'";
				}
				if( isset($attrib['inner_html']) ) {
					$html_line .= " >{$attrib['inner_html']}"
						. "</{$attrib['tag']}>";
				} else
					$html_line .= " />";
				$html['inputs'][$name]['input'] = $html_line;
			}
			// set error
			$html['inputs'][$name]['error'] =
				isset($attrib['error']) ? $attrib['error'] : '';
			// set label
			$html['inputs'][$name]['label'] = 
				isset($attrib['label']) ? $attrib['label'] : '';
		}
		// special case: hidden values
		$hidden_html_string = "";
		foreach($this->data['inputs'] as $name => &$attrib) {
			if(strtolower($attrib['type']) == 'hidden') {
				if($name == 'anti_bot_key') {
					$correct_line = $html['inputs'][$name]['input'];
					$fake_line1 = str_replace($attrib['value'], md5('dummy1'), $correct_line);
					$fake_line2 = str_replace($attrib['value'], md5('dummy2'), $correct_line);
					$hidden_html_string .= sprintf("<!--\n%s\n-->%s<!--\n%s\n-->",
						$fake_line1, $correct_line, $fake_line2);
				} else {
					$hidden_html_string .= $html['inputs'][$name]['input'];
				}
				unset($html['inputs'][$name]);
			}
		}
		// set wrapper
		$enctype_str = isset($this->data['enctype']) ?
			" enctype='{$this->data['enctype']}'" : "";
		$html['begin'] = "<form name='{$this->data['name']}' method='POST'"
			. $enctype_str
			. " action='{$this->data['url']}'>$hidden_html_string";
		if(isset($this->data['anchor']) && $this->isSubmitted()) {
			// set anchor
			$html['begin'] = "<a name='{$this->data['anchor']}'"
				. "style='height:0;margin:0;padding:0;'></a>"
				. $html['begin'];
		}
		$html['end'] = "</form>";
		if(isset($this->data['focus'])) {
			// set focus
			if(!isset($this->data['inputs'][$this->data['focus']])) {
				 // get first element
				foreach($this->data['inputs'] as $name => &$attrib) {
					$this->data['focus'] = $name;
					break;
				}
			}
			$html['end'] .= "<script> document." .
				$this->data['name'] . "." .
				$this->data['focus'] . ".focus(); </script>";
		}
		// move submit
		$html['submit'] = $html['inputs'][$this->data['submit_name']];
		unset($html['inputs'][$this->data['submit_name']]);
		return $html;
	}

	/**
	 * Get submitted values
	 * Returned values are filtered and are set to at least ""
	 *
	 * @param bool $postfilters
	 * @return array
	 */
	public function getValues($postfilters = true) {
		$values = array();
		foreach($this->data['inputs'] as $name => &$attrib) {
			if($postfilters && $name == 'anti_bot_key')
				continue;
			$values[$name] = isset($_POST[$name]) ? $_POST[$name] : "";
			// apply filters
			foreach($attrib['filters'] as $filter)
				$values[$name] = $filter->filter($values[$name]);
			// apply postfilters
			if($postfilters)
				foreach($attrib['postfilters'] as $filter)
					$values[$name] = $filter->filter($values[$name]);
		}
		return $values;
	}

	/**
	 * Set default values
	 *
	 * @param array $values
	 */
	public function setDefaultValues($values) {
		foreach($this->data['inputs'] as $name => &$attrib) {
			if(isset($values[$name]) && $name != $this->data['submit_name']) {
				// special case: checkbox
				if(strtolower($attrib['type']) == 'checkbox') {
					if($values[$name])
						$attrib['checked'] = "checked";
					else
						if(isset($attrib['checked']))
							unset($attrib['checked']);
				} else
					$attrib['value'] = $values[$name];
			}
		}
	}

	/**
	 * checks if submitted form is valid and sets
	 * default values to their submitted content
	 *
	 * @return bool
	 */
	public function isSubmittedAndValid() {
		if(!$this->isSubmitted())
			return false;
		// all inputs are filtered and set to at least ""
		$values = $this->getValues(false);
		$valid = true;
		foreach($this->data['inputs'] as $name => &$attrib) {
			// check if the input is empty
			if($values[$name] == ""){
				// error if it's required
				if($attrib['required']) {
					$attrib['error'] = "This field is required";
					$valid = false;
					if($name == 'anti_bot_key')
					    die("JForm->isSubmittedAndValid(): Error: "
					    	. "anti_bot_key is not set.");
				}
			} else { // not empty
				// apply validator
				foreach($attrib['validators'] as $validator) {
					if(!$validator->isValid($values[$name])) {
						$attrib['error'] = $validator->error;
						$valid = false;
						if($name == 'anti_bot_key')
						    die("JForm->isSubmittedAndValid(): Error: "
						    	. "anti_bot_key is invalid.");
						break;
					}
				}
			}
		}
		$this->setDefaultValues($values);
		return $valid;
	}
}



