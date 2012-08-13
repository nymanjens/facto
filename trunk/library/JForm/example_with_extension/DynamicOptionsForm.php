<?php
/**
 * Dynamic Options Form
 *   - usage: see the provided example
 *   - notice: this extension needs JQuery to be loaded
 */
class DynamicOptionsForm extends JForm {
	/**
	 * @override
	 * 
	 * @param array $data
	 */
	public function __construct(array $data) {
	    // preprocessing
	    foreach($data['inputs'] as $name => &$attrib)
	        if(isset($attrib['dynamic_options']))
	            if(!isset($attrib['options']))
	                $attrib['options'] = array();
        // parent construct
		parent::__construct($data);
		// add extra ignore
		if(!in_array('dynamic_options', self::$IGNORED_ATTRIB))
		    self::$IGNORED_ATTRIB[] = 'dynamic_options';
	}
	
	/**
	 * @override
	 *
	 * @return array
	 */
	public function getHtml() {
	    // set default dynamic_options
        foreach($this->data['inputs'] as $name => &$attrib) {
            if(isset($attrib['dynamic_options'])) {
                if(isset($this->data['inputs'][$attrib['dynamic_options']['master_input']]['value'])) {
                    $value = $this->data['inputs'][$attrib['dynamic_options']['master_input']]['value'];
	                $options = isset($attrib['dynamic_options']['options'][$value]) ?
	                    $attrib['dynamic_options']['options'][$value] : array();
                    $attrib['options'] = $options;
                }
            }
        }
        // parent call
	    $html = parent::getHtml();
	    
        /********* add dynamic functions with JQuery *********/
        // get dependencies
        $dependencies = array();
        foreach($this->data['inputs'] as $name => &$attrib) {
            if(isset($attrib['dynamic_options'])) {
                $master_input = $attrib['dynamic_options']['master_input'];
                $options = $attrib['dynamic_options']['options'];
                // parse options into option tags
                $options_html = array();
                $ignore_list = self::$IGNORED_ATTRIB;
                $ignore_list[] = 'options';
                foreach($options as $prequisite => $dependent_options) {
                    $html_line = "";
                    foreach($dependent_options as $opt_name => $opt_attrib) {
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
					$options_html[$prequisite] = $html_line;
				}
				// add to dependencies array
                if(!isset($dependencies[$master_input]))
                    $dependencies[$master_input] = array();
                $dependencies[$master_input][$name] = $options_html;
            }
        }
        // add script
        $script = <<<SCRIPT
            $(document.%s.%s).bind('change keypress', function(){
              var master_value = $(this).val();
              var dep_options = %s;
              var options = eval('dep_options.' + master_value);
              if(options == undefined)
                options = "";
              $(document.%s.%s).html(options);
            });
SCRIPT;
        $script_html = "";
        foreach($dependencies as $master_input => $dependent) {
            foreach($dependent as $dependent_input => $dep_options) {
                $json_options = json_encode($dep_options);
                $script_html .= sprintf($script,
                	$this->data['name'], $master_input, $json_options,
                    $this->data['name'], $dependent_input);
            }
        }
        $html['begin'] .= <<<SCRIPT
            <script type="text/javascript">
              $(function(){
                  $script_html
              })
            </script>
SCRIPT;
	    // return
	    return $html;
    }
	
	/**
	 * @override
	 *
	 * @param bool $postfilters
	 * @return array
	 */
	public function getValues($postfilters = true) {
	    $values = parent::getValues($postfilters);
	    // modify validators to the correct options set
	    if(!$postfilters){
	        foreach($this->data['inputs'] as $name => &$attrib){
	            if(isset($attrib['dynamic_options'])) {
	                $value = $values[$attrib['dynamic_options']['master_input']];
	                $options = isset($attrib['dynamic_options']['options'][$value]) ?
	                    $attrib['dynamic_options']['options'][$value] : array();
	                $attrib['validators'] = array(new ConstrainedSetValidator($options));
	            }
	        }
	    }
	    return $values;
    }
}


