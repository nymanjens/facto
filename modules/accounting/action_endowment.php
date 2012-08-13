<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/********* get vars *********/
$returnto = isset($_GET['returnto']) && $_GET['returnto'] != '' ? $_GET['returnto'] : ROOT_URL.'accounting/overview/liquidation';

/********* form *********/
include_once dirname(__FILE__).'/form.inc.php';
unset($account_options[COMMON_ACCOUNT]);
foreach($account_options as $key => &$val)
    $val['label'] = $PAYED_WITH_WHAT[DEFAULT_LIQUIDATION_METHOD] . ' '  . $val['label'];
// get defaults
$default_endower_name = reset(array_keys($account_options));
foreach(array_keys($account_options) as $endower_name)
    if($user->name == $endower_name)
        $default_endower_name = $endower_name;

$form = new CustomPriceForm(array(
    'focus' => '',
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
            'label' => t("From"),
            'type' => "radio",
            'required' => "true",
            'options' => $account_options,
            'value' => $default_endower_name,
        ),
        'account_to' => array(
            'label' => t("To"),
            'type' => "static",
            'value' => $PAYED_WITH_WHAT[DEFAULT_LIQUIDATION_METHOD] . ' ' . COMMON_ACCOUNT,
            'size' => "45",
        ),
        'category' => array(
            'label' => t("Category"),
            'type' => "static",
            'value' => $category_options[ENDOWMENT_CATEGORY]['label'],
            'size' => "45",
        ),
        'description' => array(
            'label' => t("Description"),
            'required' => "true",
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
            'value' => t("Endowment") . " " . $default_endower_name,
        ),
        'price' => array(
            'label' => t("Value"),
            'type' => "price",
            'value' => '0.00',
            'size' => "7",
            'required' => "true",
        ),
    )
));
if( $form->isSubmittedAndValid() ) {
    $values = $form->getValues();
    $values['category'] = ENDOWMENT_CATEGORY;
    $account = $values['account'];
    $price = $values['price'];
    create_account_input(array_merge($values, array(
        'account' => $account,
        'payed_with_what' => DEFAULT_LIQUIDATION_METHOD,
        'payed_with_whose' => $account,
        'price' => -$price,
    )));
    create_account_input(array_merge($values, array(
        'account' => COMMON_ACCOUNT,
        'payed_with_what' => DEFAULT_LIQUIDATION_METHOD,
        'payed_with_whose' => COMMON_ACCOUNT,
        'price' => $price,
    )));
    $user->setInstantMessage('message', t("Endowment of %s successfully made", CURRENCY_SYMBOL." $price"));
    redirect($returnto);
}
// output
$view->assign('selected_link', 'accounting/overview/endowments');
$view->assign('form', $form->getHtml());
$view->assign('frame_title', t("Make Endowment"));
$template = get_template("input");
$dwoo->output($template, $view);


