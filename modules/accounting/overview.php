<?php
/**
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// default paramameters
$ACCOUNT_INPUTS_LIMIT = 10;
// get control parameters
$overview_type = isset($HTTP_REQUEST[2]) ? $HTTP_REQUEST[2] : DEFAULT_OVERVIEW_TYPE;
$disable_num_limit = isset($_GET['disable_num_limit'])? $_GET['disable_num_limit']:'';
$show_all_accounts = isset($_GET['show_all_accounts'])? $_GET['show_all_accounts']:'';
// get shown accounts
$accounts = array(COMMON_ACCOUNT);
if(in_array($user->name, array_keys($ACCOUNTS)))
    $accounts[] = $user->name;
// override if $show_all_accounts == true
$accounts = $show_all_accounts ? array_keys($ACCOUNTS) : $accounts;
// set default template parameters
$template_name = 'overview_' . $overview_type;
$view->assign('selected_link', 'accounting/overview/' . $overview_type);
$view->assign('show_all_accounts', $show_all_accounts);
$view->assign('disable_num_limit', $disable_num_limit);
$view->assign('show_all_accounts_label', $show_all_accounts?t("Show main accounts"):t("Show all accounts"));
$view->assign('disable_num_limit_label', $disable_num_limit ? t("Show only %d results", $ACCOUNT_INPUTS_LIMIT)
        : t("Show more than %d results", $ACCOUNT_INPUTS_LIMIT));

// utility function
function alter_account_inputs_limit($new_limit){
    global $view, $disable_num_limit, $ACCOUNT_INPUTS_LIMIT;
    $ACCOUNT_INPUTS_LIMIT = $new_limit;
    $view->assign('disable_num_limit_label', $disable_num_limit ? t("Show only %d results", $new_limit)
        : t("Show more than %d results", $new_limit));
}
function limit($expand_keyword='none') {
    global $disable_num_limit, $ACCOUNT_INPUTS_LIMIT;
    if($disable_num_limit == $expand_keyword || $disable_num_limit == '1')
        return '';
    else
        return 'LIMIT ' . $ACCOUNT_INPUTS_LIMIT;
}
function expand_link_if_necessary($q, $expand_keyword='none', $skip_query=-1){
    global $ACCOUNT_INPUTS_LIMIT, $overview_type, $show_all_accounts;
    if(!limit($expand_keyword))
        return '';
    if($skip_query === -1) {
        $res = query($q);
        if(mysql_num_rows($res) <= $ACCOUNT_INPUTS_LIMIT)
            return '';
    } else {
        if(!$skip_query)
            return '';
    }
    return ROOT_URL.sprintf('accounting/overview/%s?disable_num_limit=%s&show_all_accounts=%s',
        $overview_type, $expand_keyword, $show_all_accounts);
}

// get data
switch($overview_type) {

case 'everything':
    alter_account_inputs_limit(50);
    $list = array();
    $q = sprintf("SELECT * FROM ".TAG."account_inputs WHERE category!='[BALANCE_SET]' ORDER BY timestamp DESC, creation_time DESC %s", limit());
    $res = query($q);
    $list = fetch_all_account_inputs($res);
    $view->assign('list', $list);
    $view->assign('actions_hide_show_all_accounts', true);
    $view->assign('actions_show_withdrawal_button', true);
    $view->assign('actions_show_endowment_button', true);
    $view->assign('expand_link', expand_link_if_necessary(sprintf("SELECT * FROM ".TAG."account_inputs WHERE category!='[BALANCE_SET]'")));
    break;

case 'income_expenses':
    alter_account_inputs_limit(20);
    $lists = array();
    foreach($accounts as $account) {
        $q = sprintf("SELECT * FROM ".TAG."account_inputs WHERE category!='%s' AND category!='[BALANCE_SET]'
            AND account='%s' ORDER BY timestamp DESC, creation_time DESC %s", ACCOUNTING_CATEGORY, $account, limit($account));
        $res = query($q);
        $lists[] = array(
            'title' => t("Income/Expenses") . ": " . $account,
            'list' => fetch_all_account_inputs($res),
            'expand_link' => expand_link_if_necessary(
                sprintf("SELECT * FROM ".TAG."account_inputs WHERE category!='%s' AND category!='[BALANCE_SET]'
                    AND account='%s'", ACCOUNTING_CATEGORY, $account),
                $account
            ),
        );
    }
    $view->assign('lists', $lists);
    break;

case 'cash_flow':
    $lists = array();
    foreach($accounts as $account) {
        $list = array();
        foreach($ACCOUNT_METHOD_COMBINATIONS as $combination) {
            $method = $combination[0];
            if($account != $combination[1])
                continue;
            // get inputs
            $q = sprintf("SELECT * FROM ".TAG."account_inputs WHERE payed_with_whose='%s' AND payed_with_what='%s'
                ORDER BY timestamp DESC, creation_time DESC %s", strtolower($account), $method, limit($account.$method));
            $res = query($q);
            $inputs = fetch_all_account_inputs($res);
            // add balance info
            add_balance_information($inputs, $method, $account);
            $list[$method] = array(
                'title' => t("Cash flow") . " " . $account . ": " . $CASH_FLOW_LABELS[$method],
                'method' => $method,
                'account' => $account,
                'list' => $inputs,
                'default_balance' => count($inputs) > 0 ? $inputs[reset(array_keys($inputs))]['balance'] : '0.00',
                'expand_link' => expand_link_if_necessary(
                    sprintf("SELECT * FROM ".TAG."account_inputs WHERE payed_with_whose='%s' AND payed_with_what='%s'",
                        strtolower($account), $method),
                    $account.$method
                ),
            );
        }
        $lists[] = $list;
    }
    $view->assign('lists', $lists);
    $view->assign('actions_show_withdrawal_button', true);
    break;

case 'liquidation':
    // default $show_all_accounts is true
    if(!isset($_GET['show_all_accounts']))
       $show_all_accounts = true; 
    
    $lists = array();
    $q = "SELECT * FROM ".TAG."account_inputs WHERE category!='[BALANCE_SET]' ORDER BY timestamp ASC, creation_time ASC";
    $res = query($q);
    $master_inputs = fetch_all_account_inputs($res);
    // get accounts
    $permutations = array();
    foreach(array_keys($ACCOUNTS) as $account)
        $permutations[] = array($user->name, $account);
    if($show_all_accounts) {
        foreach(array_keys($ACCOUNTS) as $account1)
            foreach(array_keys($ACCOUNTS) as $account2)
                if($account1 != $user->name && $account2 != $user->name) {
                    $ok = true;
                    foreach($permutations as $perm) {
                        list($a1, $a2) = $perm;
                        if($a1 == $account2 && $a2 == $account1)
                            $ok = false;
                    }
                    if($ok)
                        $permutations[] = array($account1, $account2);
                }
    }
    
    // cycle through all
    foreach($permutations as $perm) {
        list($account1, $account2) = $perm;
        // no liquidation with self
        if($account1 == $account2)
            continue;
        $inputs = $master_inputs;
        foreach($inputs as $key => &$in) {
            $A = strtolower($in['payed_with_whose']);
            $B = strtolower($in['account']);
            $X = strtolower($account1);
            $Y = strtolower($account2);
            if( ($A == $X && $B == $Y) || ($A == $Y && $B == $X))
                continue;
            else
                unset($inputs[$key]);
        }
        // calculate liquidation sum
        $total = 0;
        foreach($inputs as $key => &$in) {
            $total += $in['account'] == $account1 ? -$in['price'] : $in['price'];
            $in['total'] = sprintf("%.2f", $total);
        }
        // truncate lists
        $truncated_list = false;
        if(limit($account1.$account2)) {
            while(count($inputs) > $ACCOUNT_INPUTS_LIMIT) {
                array_shift($inputs);
                $truncated_list = true;
            }
        }
        // reverse list
        $inputs = array_reverse($inputs);
        $lists[] = array(
            'title' => t("Liquidation") . sprintf(": %s - %s", $account1, $account2),
            'totaltitle' => sprintf("%s -> %s", shortname($account1, $very_short=True), shortname($account2, $very_short=True)),
            'list' => $inputs,
            'account1' => $account1,
            'account2' => $account2,
            'total' => sprintf("%.2f", $total),
            'expand_link' => expand_link_if_necessary('', $account1.$account2, $truncated_list),
        );
    }
    $view->assign('lists', $lists);
    $view->assign('actions_hide_new_entry', true);
    $view->assign('actions_hide_show_all_accounts', true);
    break;

case 'endowments':
    alter_account_inputs_limit(20);
    $lists = array();
    $accounts = array_keys($ACCOUNTS);
    foreach($accounts as $account) {
        $q = sprintf("SELECT * FROM ".TAG."account_inputs WHERE category='%s' AND account='%s'
            ORDER BY timestamp DESC, creation_time DESC %s", ENDOWMENT_CATEGORY, $account, limit($account));
        $res = query($q);
        $lists[] = array(
            'title' => t("Endowments") . ": " . $account,
            'list' => fetch_all_account_inputs($res),
            'expand_link' => expand_link_if_necessary(
                sprintf("SELECT * FROM ".TAG."account_inputs WHERE category='%s' AND account='%s'", ENDOWMENT_CATEGORY, $account),
                $account
            ),
        );
    }
    $view->assign('lists', $lists);
    $view->assign('actions_hide_show_all_accounts', true);
    $view->assign('actions_hide_new_entry', true);
    $view->assign('actions_show_endowment_button', true);
    break;

case 'summary':
    $lists = array();
    foreach($accounts as $account) {
        $matrix = array();
        $years = array();
        $q = sprintf("SELECT * FROM ".TAG."account_inputs WHERE account='%s'
            ORDER BY timestamp ASC, creation_time ASC", $account);
        $res = query($q);
        while($elem = mysql_fetch_array($res)) {
            add_info_to_account_input_row($elem);
            // filter exception categories
            if($elem['category'] == '[BALANCE_SET]')
                continue;
            // get category, year and month
            $cat_code = $elem['category'];
            $category = $CATEGORIES[$elem['category']]['name'];
            $year = date('Y', $elem['timestamp']);
            $month = t(date('M', $elem['timestamp']));
            // add price to right spot in 3D $matrix
            if(!isset($matrix[$cat_code]))
                $matrix[$cat_code] = array();
            if(!isset($matrix[$cat_code][$year]))
                $matrix[$cat_code][$year] = array();
            if(!isset($matrix[$cat_code][$year][$month]))
                $matrix[$cat_code][$year][$month] = array(
                    'totexp' => 0, // total expediture
                    'elems' => array(),
                );
            $matrix[$cat_code][$year][$month]['totexp'] += $elem['price'];
            $matrix[$cat_code][$year][$month]['elems'][] = $elem;
            // add year+month to $years
            if(!isset($years[$year]))
                $years[$year] = array();
            if(!in_array($month, $years[$year]))
                $years[$year][] = $month;
        }
        // order by category as in preordered array $CATEGORIES
        // fill in date gaps
        $ordered_matrix = array();
        foreach($CATEGORIES as $cat_code => $category) {
            if(!isset($matrix[$cat_code]))
                continue;
            $ordered_matrix[$cat_code] = array();
            foreach($years as $year => $months) {
                $ordered_matrix[$cat_code][$year] = array();
                foreach($months as $month) {
                    if(isset($matrix[$cat_code][$year][$month])) {
                        $matrix[$cat_code][$year][$month]['totexp_str'] = sprintf("%.2f", $matrix[$cat_code][$year][$month]['totexp']);
                        $ordered_matrix[$cat_code][$year][] = $matrix[$cat_code][$year][$month];
                    } else
                        $ordered_matrix[$cat_code][$year][] = array();
                }
            }
        }
        // calculate totals
        $totals = array();
        foreach($years as $year => $months) {
            $totals[$year] = array();
            foreach($months as $month) {
                $total = 0;
                foreach($CATEGORIES as $cat_code => $category) {
                    if($cat_code == ACCOUNTING_CATEGORY)
                        continue;
                    if($cat_code == ENDOWMENT_CATEGORY && $account == COMMON_ACCOUNT)
                        continue;
                    if(!isset($matrix[$cat_code]))
                        continue;
                    if(isset($matrix[$cat_code][$year][$month]))
                        $total += $matrix[$cat_code][$year][$month]['totexp'];
                }
                $totals[$year][$month] =  sprintf("%.2f", $total);
            }
        }
        // calculate averages
        $averages = array();
        $first = true;
        foreach($years as $year => $months) {
            // also count first month?
            $count_first_month = !$first;
            $first = false;
            if(!$count_first_month)
                array_shift($months);
            // also count last month?
            $count_last_month = !($year == date('Y'));
            if(!$count_last_month)
                array_pop($months);
            // get num months
            $averages_per_category = array();
            foreach($CATEGORIES as $cat_code => $category) {
                $sum = 0;
                if(!isset($matrix[$cat_code]))
                    continue;
                foreach($months as $month) {
                    if(isset($matrix[$cat_code][$year][$month]))
                        $sum += $matrix[$cat_code][$year][$month]['totexp'];
                }
                $averages_per_category[$cat_code] =  count($months) ? sprintf("%.2f", $sum/count($months)) : '-';
            }
            // get avg of total
            $sum = 0;
            foreach($months as $month) {
                if(isset($totals[$year][$month]))
                    $sum += $totals[$year][$month];
            }
            $averages_per_category['total'] =  count($months) ? sprintf("%.2f", $sum/count($months)) : '-' ;
            $averages[$year] = $averages_per_category;
        }
        // append
        $lists[] = array(
            'title' => t("Summary"). ": " . $account,
            'matrix' => $ordered_matrix,
            'years' => $years,
            'totals' => $totals,
            'averages' => $averages,
        );
    }
    // function for dwoo
    function count_and_add_1($a){return count($a)+1;}
    $view->assign('lists', $lists);
    $view->assign('actions_hide_new_entry', true);
    // show all years
    $view->assign('disable_num_limit_label', $disable_num_limit ? t("Show last year") : t("Show all years"));
    $view->assign('show_all_years', $disable_num_limit);
    // focus year: open up one year
    $view->assign('focus_year', isset($_GET['focus_year'])? $_GET['focus_year']+0 : 0);
    break;

default:
    die('Error: accounting->overview: wrong overview type');
}

// output
$view->assign('self_path', ROOT_URL.'accounting/overview/'.$overview_type);
$template = get_template($template_name);
$dwoo->output($template, $view);


