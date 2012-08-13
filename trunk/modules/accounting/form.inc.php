<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// account options
$account_options = array();
foreach(array_keys($ACCOUNTS) as $opt)
    $account_options[$opt] = array('label' => $opt);

// category options
$category_options = array();
foreach($CATEGORIES as $key => $val) {
    $label = isset($val['help']) && $val['help'] != '' ? sprintf("%s (%s)", $val['name'], $val['help']) : $val['name'];
    $category_options[$key] = array('label' => $label);
}

// category dynamic options
$category_dynamic_options = array();
foreach($ACCOUNTS as $account => $categories) {
    $category_dynamic_options[$account] = array();
    foreach($categories as $category)
        $category_dynamic_options[$account][$category] = $category_options[$category];
}
$category_dynamic_options = array(
    'master_input' => 'account',
    'options' => $category_dynamic_options,
);

// payed with options (preferred combinations)
$payed_with_options = array();
foreach($ACCOUNT_METHOD_COMBINATIONS as $combination) {
    $what = $combination[0];
    $whose = $combination[1];
    $what_label = $PAYED_WITH_WHAT[$what];
    $payed_with_options[$what.'_'.$whose] = array('label' => $what_label . ' ' . $whose);
}



$form_data = array(
    'focus' => "",
    'inputs' => array(
        'timestamp' => array(
            'label' => t("Date"),
            'required' => "true",
            'value' => date('Y-m-d'),
            'filters' => new DefaultTextFilter(),
            'validators' => new DateValidator(),
            'postfilters' => new DateToTimestampPostFilter(),
        ),
        'issuer' => array(
            'label' => t("Issuer"),
            'type' => "static",
            'value' => $user->name,
        ),
        'account' => array(
            'label' => t("Beneficiary"),
            'type' => "radio",
            'required' => "true",
            'options' => $account_options,
            'value' => reset(array_keys($account_options)),
        ),
        'payed_with' => array(
            'label' => t("Payed with/to"),
            'type' => "radio",
            'required' => "true",
            'options' => $payed_with_options,
            'value' => reset(array_keys($payed_with_options)),
            'postfilters' => new ExtractPayedWithWhatAndWhoPostFilter(),
        ),
        'category' => array(
            'label' => t("Category"),
            'type' => "select",
            'required' => "true",
            'options' => $category_options,
            'dynamic_options' => $category_dynamic_options,
        ),
        'description' => array(
            'label' => t("Description"),
            'required' => "true",
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
        ),
        'price' => array(
            'type' => "price",
            'label' => t("Flow"),
            'size' => "7",
            'required' => "true",
            'sign_origin' => array(
                'master_input' => 'category',
                'sign_relation' => $CATEGORIES,
            ),
        ),
    ),
);

// if(!defined('ACCOUNT_INPUT_FORM_NEW')) {
//    unset($form_data['inputs']['category']['dynamic_options']);
// }

$account_input_form = new CustomPriceForm( $form_data );

