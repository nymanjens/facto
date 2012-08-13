<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/********* get vars *********/
if(!isset($HTTP_REQUEST[2]))
	die('Error: accounting->liquidate: req2 not set');
if(!isset($HTTP_REQUEST[3]))
	die('Error: accounting->liquidate: req3 not set');
if(!isset($HTTP_REQUEST[4]))
	die('Error: accounting->liquidate: req4 not set');

$account1 = $HTTP_REQUEST[2];
$account2 = $HTTP_REQUEST[3];
$default_flow = $HTTP_REQUEST[4];
$returnto = isset($_GET['returnto']) && $_GET['returnto'] != '' ? $_GET['returnto'] : ROOT_URL.'accounting/overview/liquidation';

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
        'from' => array(
            'label' => t("From"),
            'type' => "radio",
            'required' => "true",
            'options' => $payed_with_options,
            'value' => DEFAULT_LIQUIDATION_METHOD . '_' . $account1,
            'postfilters' => new ExtractPayedWithWhatAndWhoPostFilter(),
        ),
        'to' => array(
            'label' => t("To"),
            'type' => "radio",
            'required' => "true",
            'options' => $payed_with_options,
            'value' => DEFAULT_LIQUIDATION_METHOD . '_' . $account2,
            'postfilters' => new ExtractPayedWithWhatAndWhoPostFilter(),
        ),
        'description' => array(
            'label' => t("Description"),
            'required' => "true",
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
            'value' => t("Liquidation"),
        ),
        'price' => array(
            'label' => t("Value"),
            'type' => "price",
            'value' => $default_flow,
            'size' => "7",
            'required' => "true",
        ),
    )
));
if( $form->isSubmittedAndValid() ) {
    $values = $form->getValues();
    list($from_method, $from_account) = $values['from'];
    list($to_method, $to_account) = $values['to'];
    $values['category'] = ACCOUNTING_CATEGORY;
    $price = $values['price'];
    create_account_input(array_merge($values, array(
        'account' => $to_account,
        'payed_with_what' => $to_method,
        'payed_with_whose' => $to_account,
        'price' => $price,
    )));
    create_account_input(array_merge($values, array(
        'account' => $to_account,
        'payed_with_what' => $from_method,
        'payed_with_whose' => $from_account,
        'price' => -$price,
    )));
    $user->setInstantMessage('message', t("Liquidation of %s successfully completed",  CURRENCY_SYMBOL." $price"));
    redirect($returnto);
}
// output
$view->assign('selected_link', 'accounting/overview/liquidation');
$view->assign('form', $form->getHtml());
$view->assign('frame_title', t("Liquidate"));
$template = get_template("input");
$dwoo->output($template, $view);


