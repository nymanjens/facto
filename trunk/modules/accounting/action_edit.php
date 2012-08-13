<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/********* get vars *********/
if(!isset($HTTP_REQUEST[2]))
	die('Error: accounting->edit: No action set');
if(!isset($HTTP_REQUEST[3]))
	die('Error: accounting->edit: No input ID set');
$action = $HTTP_REQUEST[2];
$input_id = $HTTP_REQUEST[3];
if(!is_numeric($input_id))
	die('Error: accounting->edit: Input ID has to be numeric');
$returnto = isset($_GET['returnto']) && $_GET['returnto'] != '' ? $_GET['returnto'] : ROOT_URL.'accounting/overview';

/********* delete *********/
if($action == 'delete'){
    $q = "DELETE FROM ".TAG."account_inputs WHERE id=$input_id";
    query_with_log($q);
    $user->setInstantMessage('message', t("Entry removed"));
    redirect($returnto);
}

/********* edit *********/
if($action == 'edit'){
    include_once dirname(__FILE__).'/form.inc.php';
    $q = "SELECT * FROM ".TAG."account_inputs WHERE id=$input_id";
    $elem = mysql_fetch_array(query($q));
    $timestamp = $elem['timestamp'];
    $elem['timestamp'] = date('Y-m-d', $elem['timestamp']);
    $elem['issuer'] = get_username_from_id($elem['issuer']);
    $elem['payed_with'] = $elem['payed_with_what'] . '_' . $elem['payed_with_whose'];
    $account_input_form->setDefaultValues($elem);
    
    if( $account_input_form->isSubmittedAndValid() ) {
        $values = $account_input_form->getValues();
        list($payed_with_what, $payed_with_whose) = $values['payed_with'];
        // fix that time shifts with edit
        if(date('Y-m-d', $timestamp) == date('Y-m-d', $values['timestamp']))
            $values['timestamp'] = $timestamp;
        $q = sprintf("UPDATE ".TAG."account_inputs SET account='%s', category='%s',
            payed_with_what='%s', payed_with_whose='%s', description='%s', price='%f', timestamp='%d' WHERE id=$input_id",
            $values['account'], $values['category'], $payed_with_what, $payed_with_whose, $values['description'],
            $values['price'], $values['timestamp']);
        query_with_log($q);
        $user->setInstantMessage('message', t("Entry changed"));
        redirect($returnto);
    }
    
    // output
    $view->assign('selected_link', 'accounting/overview');
    $view->assign('form', $account_input_form->getHtml());
    $view->assign('frame_title', t("Edit Entry"));
    $template = get_template("input");
    $dwoo->output($template, $view);
}


