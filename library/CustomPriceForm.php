<?php
/**
 * Custom Price Form
 *   - usage: TODO
 *   - notice: this extension needs JQuery to be loaded
 */
class CustomPriceForm extends DynamicOptionsForm {
    /**
     * @override
     * 
     * @param array $data
     */
    public function __construct(array $data) {
        // add extra ignore
        if(!in_array('sign_origin', self::$IGNORED_ATTRIB))
            self::$IGNORED_ATTRIB[] = 'sign_origin';
        // parent construct
        parent::__construct($data);
        // process price type
        foreach($this->data['inputs'] as $name => &$attrib) {
            if(strtolower($attrib['type']) == 'price') {
                $attrib['type'] = "text";
                $attrib['autocomplete'] = "off";
                $attrib['validators'][] = new PriceValidator();
                $attrib['filters'][] = new PriceFilter();
                if($attrib['value'] == '')
                    $attrib['value'] = '-0.00';
            }
        }
    }
    
    /**
     * @override
     *
     * @return array
     */
    public function getHtml() {
        // parent call
        $html = parent::getHtml();
        
        /********* add dynamic functions with JQuery *********/
        // get sign relation
        $dependencies = array();
        foreach($this->data['inputs'] as $name => &$attrib) {
            if(isset($attrib['sign_origin'])) {
                $master_input = $attrib['sign_origin']['master_input'];
                $sign_relation = $attrib['sign_origin']['sign_relation'];
                // parse sign relation
                $parsed_relation = array();
                foreach($sign_relation as $category => $arr)
                    $parsed_relation[$category] = isset($arr['default_positive_price']) &&
                        $arr['default_positive_price'] ? true : false;
                    
                // add to dependencies array
                if(!isset($dependencies[$master_input]))
                    $dependencies[$master_input] = array();
                $dependencies[$master_input][$name] = $parsed_relation;
            }
        }
        // add script
        $script = <<<SCRIPT
            $(document.%s.%s).bind('change keypress', function(){
              var master_value = $(this).val();
              var dep_options = %s;
              var positive_sign = eval('dep_options.' + master_value);
              if(positive_sign == undefined)
                positive_sign = false;
              var price_input = $(document.%s.%s);
              var price = price_input.val();
              var sign = positive_sign ? '' : '-';
              if(price == '' || price == '0.00' || price == '-0.00')
                price = sign + '0.00';
              price_input.val(price);
            }).trigger('change');
            $(document.%s.%s).bind('focus', function(){
              var value = $(this).val();
              if(value == '') {
                value = '0.00';
                $(this).val(value);
              }
              if(value.charAt(0) == '-' || value.charAt(0) == '+') {
                // note: this doesn't work yet
                $(this).caret(1, value.length);
              }
            });
SCRIPT;
        $script_html = "";
        foreach($dependencies as $master_input => $dependent) {
            foreach($dependent as $dependent_input => $dep_options) {
                $json_options = json_encode($dep_options);
                $script_html .= sprintf($script,
                    $this->data['name'], $master_input,
                    $json_options,
                    $this->data['name'], $dependent_input,
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
}


