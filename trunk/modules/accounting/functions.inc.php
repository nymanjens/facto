<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/**
 * fetch all mysql results for account inputs
 * 
 * @param $res: mysql result
 * @return array
 */
function fetch_all_account_inputs($res) {
    $list = array();
    while($elem = mysql_fetch_array($res)){
        add_info_to_account_input_row($elem);
        $list[] = $elem;
    }
    return $list;
}

/**
 * add extra info to row
 * 
 * @param $res: mysql result
 * @return array
 */
function add_info_to_account_input_row(&$elem) {
    global $CATEGORIES, $PAYED_WITH_WHAT;
    $elem['issuer_name'] = get_username_from_id($elem['issuer']);
    if($elem['category'] != '[BALANCE_SET]')
        $elem['category_name'] = $CATEGORIES[$elem['category']]['name'];
    $elem['time'] = time_to_string($elem['timestamp']);
    $elem['payed_with_name'] = $PAYED_WITH_WHAT[$elem['payed_with_what']] . ' ' . shortname($elem['payed_with_whose']);
    $elem['account_short'] = shortname($elem['account']);
    $elem['price_str'] = sprintf("%.2f", $elem['price']);
    $elem['edit_link'] = ($elem['category'] != '[BALANCE_SET]') ? ROOT_URL."accounting/edit/edit/{$elem['id']}?returnto=".self_url()
                                                                : ROOT_URL."accounting/set_balance/edit/{$elem['id']}";
    $elem['html_edit_delete'] = sprintf(
        '<div class="buttonleft"><a class="small_edit_button" href="%s">*</a></div>'
      . '<div class="buttonleft"><a class="small_remove_button" href="%s" onclick="return '
      .   'confirm(\''. t('Are you sure you want to delete this row?\n\nThis action is irrevertable.') .'\')">X</a></div>',
        $elem['edit_link'], ROOT_URL."accounting/edit/delete/{$elem['id']}?returnto=".self_url());
}


/**
 * add balance information
 */
function add_balance_information(&$list, $method, $account) {
    $q = "SELECT * FROM ".TAG."account_inputs WHERE payed_with_what='$method' AND payed_with_whose='$account' ORDER BY timestamp, creation_time";
    $res = query($q);
    // get balance at each point
    $balance = 0;
    $balance_arr = array();
    while($elem = mysql_fetch_array($res)){
        if($elem['category'] == '[BALANCE_SET]')
            $balance = $elem['price'];
        else
            $balance += $elem['price'];
        $balance_arr[$elem['id']] = sprintf('%.2f', $balance);
    }
    // put balance info in list
    foreach($list as &$elem)
        $elem['balance'] = $balance_arr[$elem['id']];
    // merge redundant balance corrections (redundant = correct)
    $elem_keys = array_keys($list);
    foreach($list as &$elem) {
        // get next element
        $nextkey = next($elem_keys);
        if($nextkey === FALSE)
            continue; // last element is never redundant
        $nextelem = &$list[$nextkey];
        // check if this is redundant balance correction
        if($elem['category'] == '[BALANCE_SET]'
                && $nextelem['balance'] == $elem['balance']) {
            $nextelem['verified_balance'] = true;
            unset($list[prev($elem_keys)]); next($elem_keys);
        }
            
    }
}

/**
 * create account input
 */
function create_account_input($arr, $account = null, $category = null, $payed_with_what = null, $payed_with_whose = null,
        $description = null, $price = null, $timestamp = null) {
    global $user;
    if($account == null) $account = $arr['account'];
    if($category == null) $category = $arr['category'];
    if($payed_with_what == null) $payed_with_what = $arr['payed_with_what'];
    if($payed_with_whose == null) $payed_with_whose = $arr['payed_with_whose'];
    if($description == null) $description = $arr['description'];
    if($price == null) $price = $arr['price'];
    if($timestamp == null) $timestamp = $arr['timestamp'];
    // get id
    $id = get_next_table_id('account_inputs');
    // do query
    $q = sprintf("INSERT INTO ".TAG."account_inputs
        (id, issuer, account, category, payed_with_what, payed_with_whose, description, price, timestamp, creation_time)
        VALUES (%d, '%s', '%s', '%s', '%s', '%s', '%s', %s, %d, %d)", $id, $user->id, $account,
        $category, $payed_with_what, $payed_with_whose, $description, $price, $timestamp, time());
    query_with_log($q);
}




