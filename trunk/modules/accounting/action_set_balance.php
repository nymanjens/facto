<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/********* get vars *********/
if(!isset($HTTP_REQUEST[2]))
	die('Error: accounting->set_balance: req2 not set');
if(!isset($HTTP_REQUEST[3]))
	die('Error: accounting->set_balance: req3 not set');

$action = $HTTP_REQUEST[2];
$input_id = $HTTP_REQUEST[3];
$default_balance = isset($HTTP_REQUEST[4]) ? $HTTP_REQUEST[4] : '0.00';
if($action == 'edit'){
    if(!is_numeric($input_id))
    	die('Error: accounting->set_balance: Input ID has to be numeric');
    $q = "SELECT * FROM ".TAG."account_inputs WHERE id=$input_id";
    $elem = mysql_fetch_array(query($q));
    $method = $elem['payed_with_what'];
    $account = $elem['payed_with_whose'];
} else {
    $action = 'new';
    $method = $HTTP_REQUEST[2];
    $account = $HTTP_REQUEST[3];
    if(!in_array($method, array_keys($PAYED_WITH_WHAT)))
    	die('Error: accounting->set_balance: Invalid method');
    if(!in_array($account, array_keys($ACCOUNTS)))
    	die('Error: accounting->set_balance: Invalid account');
}
$returnto = isset($_GET['returnto']) && $_GET['returnto'] != '' ? $_GET['returnto'] : ROOT_URL.'accounting/overview/cash_flow';

/********* form *********/
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
        'cash_flow' => array(
            'label' => t('Cash flow'),
            'type' => "static",
            'value' => $account . ': ' . $CASH_FLOW_LABELS[$method],
        ),
        'price' => array(
            'label' => t("Balance"),
            'type' => "price",
            'size' => "7",
            'required' => "true",
            'value' => $default_balance,
        ),
    )
));

/********* new or edit *********/
if($action == 'edit') {
    $timestamp = $elem['timestamp'];
    $elem['timestamp'] = date('Y-m-d', $elem['timestamp']);
    $elem['issuer'] = get_username_from_id($elem['issuer']);
    $elem['payed_with'] = $elem['payed_with_what'] . '_' . $elem['payed_with_whose'];
    $form->setDefaultValues($elem);
}

if( $form->isSubmittedAndValid() ) {
    $values = $form->getValues();
    if($action == 'edit') {
        // fix that time shifts with edit
        if(date('Y-m-d', $timestamp) == date('Y-m-d', $values['timestamp']))
            $values['timestamp'] = $timestamp;
        $q = sprintf("UPDATE ".TAG."account_inputs SET price='%f', timestamp='%d' WHERE id=$input_id",
            $values['price'], $values['timestamp']);
    } else {
        $id = get_next_table_id('account_inputs');
        $q = sprintf("INSERT INTO ".TAG."account_inputs
            (id, issuer, account, category, payed_with_what, payed_with_whose, price, timestamp)
            VALUES (%d, '%s', '%s', '%s', '%s', '%s', %s, %d)", $id, $user->id, '', '[BALANCE_SET]', $method, $account,
            $values['price'], $values['timestamp']);
    }
    query_with_log($q);
    $user->setInstantMessage('message', t("Balance set successfully"));
    redirect($returnto);
}
// output
$view->assign('selected_link', 'accounting/overview/cash_flow');
$view->assign('form', $form->getHtml());
if($action == 'edit')
    $view->assign('frame_title', t("Edit balance"));
else
    $view->assign('frame_title', t("Set balance"));
$template = get_template("input");
$dwoo->output($template, $view);


