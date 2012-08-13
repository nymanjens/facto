<?php
/**
 * JForm example
 * most of the JForm functionality is showed here 
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 * @example
 */

// example uses dwoo template engine
include dirname(__FILE__).'/../../dwoo/dwooAutoload.php';
// JForm autoloader
include dirname(__FILE__).'/../autoload.php';
// DynamicOptionsForm
include dirname(__FILE__).'/DynamicOptionsForm.php';

// create new form
$form = new DynamicOptionsForm( array(
    'inputs' => array(
        'independent_input' => array(
            'type' => "radio",
            'value' => "chapter_2",
            'options' => array(
                'chapter_1' => array(),
                'chapter_2' => array(),
                'chapter_3' => array(),
            ),
        ),
        'dependent_input' => array(
            'type' => "select",
            'dynamic_options' => array(
                'master_input' => "independent_input",
                'options' => array(
                    '' => array(
                        '' => array(
                            'label' => '--------'
                        ),
                    ),
                    'chapter_1' => array(
                        'section_1_1' => array(),
                        'section_1_2' => array(),
                    ),
                    'chapter_2' => array(
                        'section_2_1' => array(
                            'label' => 'Section 2.1'
                        ),
                        'section_2_2' => array(),
                    ),
                ),
            ),
        ),
    )
));


if( $form->isSubmittedAndValid() ) {
    echo "This form is submitted and valid<br /><pre>";
    print_r($form->getValues());
    die("</pre>");
}

$dwoo = new Dwoo();
$tpl = new Dwoo_Template_File(dirname(__FILE__).'/index.tpl');

$view = new Dwoo_Data();
$view->assign('form', $form->getHtml());

$dwoo->output($tpl, $view);

?>
