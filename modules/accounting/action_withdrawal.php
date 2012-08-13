<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/********* get vars *********/
$returnto = isset($_GET['returnto']) && $_GET['returnto'] != '' ? $_GET['returnto'] : ROOT_URL.'accounting/overview/liquidation';

/********* get options *********/
// account from/to options
$account_from_options = array();
$account_to_options = array();
foreach($ACCOUNT_METHOD_COMBINATIONS as $combination) {
    $method = $combination[0];
    $account = $combination[1];
    if($method == DEFAULT_LIQUIDATION_METHOD)
        $account_from_options[$account] = array('label' => $account);
    if($method == CASH_METHOD_CODE)
        $account_to_options[$account] = array('label' => $account);
}

/********* form *********/
include_once dirname(__FILE__).'/form.inc.php';
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
        'account_from' => array(
            'label' => t("From account"),
            'type' => "radio",
            'required' => "true",
            'options' => $account_from_options,
            'value' => reset(array_keys($account_from_options)),
        ),
        'account_to' => array(
            'type' => "radio",
            'label' => t("To cash"),
            'required' => "true",
            'options' => $account_to_options,
            'value' => reset(array_keys($account_to_options)),
        ),
        'description' => array(
            'label' => t("Description"),
            'required' => "true",
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
            'value' => t("Withdrawal"),
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
    $values['category'] = ACCOUNTING_CATEGORY;
    $account_from = $values['account_from'];
    $account_to = $values['account_to'];
    $price = $values['price'];
    create_account_input(array_merge($values, array(
        'account' => $account_from,
        'payed_with_what' => DEFAULT_LIQUIDATION_METHOD,
        'payed_with_whose' => $account_from,
        'price' => -$price,
    )));
    create_account_input(array_merge($values, array(
        'account' => $account_from,
        'payed_with_what' => CASH_METHOD_CODE,
        'payed_with_whose' => $account_to,
        'price' => $price,
    )));
    $user->setInstantMessage('message', t("Withdrawal successfully inserted of %s", CURRENCY_SYMBOL." $price"));
    redirect($returnto);
}
// output
$view->assign('selected_link', 'accounting/overview/cash_flow');
$view->assign('form', $form->getHtml());
$view->assign('frame_title', t("Insert Withdrawal"));
$template = get_template("input");
$dwoo->output($template, $view);


