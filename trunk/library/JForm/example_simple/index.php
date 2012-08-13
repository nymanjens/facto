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

// create new form
$form = new JForm( array(
	'focus' => "", // focus first element
	// 'anchor' => "", // submit url will point to anchor at $form.begin
	// 'enctype' => "multipart/form-data", // for file input
	'submit_label' => "Submit",
	'inputs' => array(
		'login_name' => array(
			'type' => "text", // default
			'required' => "true",
			'filters' => array(new DefaultTextFilter()), // validators, filters and postfilters are put in array
														 // automatically
			'postfilters' => new SqlPostFilter(),
			'maxlength' => 10, // automatically adds a CutOffFilter
			'style' => "border-color: blue;", // custom options for the <input> tag are added simply by
											  // adding them to this array
		),
		'password' => array(
			'type' => "password",
			'required' => "true",
			'validators' => new PasswordValidator(3,8),
			'postfilters' => new Md5PostFilter(),
		),
		'email' => array(
			'label' => "E-mail",
			'filters' => new DefaultTextFilter(),
			'validators' => new EmailValidator(),
		),
		'favourite_number' => array(
			'validators' => new NumericValidator(),
		),
		'never_changing' => array(
			'type' => "hidden",
			'value' => "HIDDEN_VALUE",
		),
		'comments' => array(
			'type' => "textarea",
			'value' => "put your comments here",
			'rows' => "5",
			'cols' => "60",
			'filters' => new DefaultTextFilter(),
			'postfilters' => new TextareaPostFilter(),
		),
		'mailme' => array(
			'type' => "radio",
			'label' => "Mail me",
			'value' => 'always',
			'required' => true,
			'html-left' => "", // default
			'html-right' => "<br />", // default
			'html-center' => " ", // default
			'options' => array(
				'always' => array(
					'label' => "Always (recommended)",
				),
				'sometimes' => array(),
				'never' => array(),
			),
		),
		'favourite_season' => array(
			'type' => "select",
			'value' => 'summer',
			'options' => array(
				'winter' => array(
					'label' => "Winter (cold)",
				),
				'spring' => array(),
				'summer' => array(),
				'autumn' => array(),
			),
		),
	)
));

// setDefaultValues() was made to easily edit existing data in a form. The recommended
// approach is to put the real defaults directly into the form and put the loaded data
// into the form via this method.
$form->setDefaultValues(array(
	'comments' => "This comment was loaded from a database table.",
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
