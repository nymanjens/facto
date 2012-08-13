<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/********* get vars *********/
$returnto = isset($_GET['returnto']) && $_GET['returnto'] != '' ? $_GET['returnto'] : ROOT_URL.'accounting/overview';
include_once dirname(__FILE__).'/form.inc.php';

/********* form *********/
if( $account_input_form->isSubmittedAndValid() ) {
    $values = $account_input_form->getValues();
    list($payed_with_what, $payed_with_whose) = $values['payed_with'];
    create_account_input(array_merge($values, array(
        'payed_with_what' => $payed_with_what,
        'payed_with_whose' => $payed_with_whose,
    )));
    $user->setInstantMessage('message', t("Input added"));
    redirect($returnto);
}

/********* output *********/
$view->assign('selected_link', 'accounting/input');
$view->assign('form', $account_input_form->getHtml());
$view->assign('frame_title', t("Input"));
$template = get_template("input");
$dwoo->output($template, $view);

