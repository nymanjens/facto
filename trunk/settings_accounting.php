<?php
/**
 * accounting settings
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/*** constants -- do not change ***/
define('ACCOUNTING_CATEGORY', "ACC"); // code of accounting category
define('ENDOWMENT_CATEGORY', "ENDOW"); // code of endowment category (for transfer of money to common account)

/*** functional settings ***/
define('DEFAULT_OVERVIEW_TYPE', 'cash_flow'); // defines the first page a user views upon login

// income/expense categories
$CATEGORIES = array(
    'LIFE' => array(
        'name' => "Groceries",
        'help' => "Food, Toothpaste, ..",
    ),
    'FUN' => array(
        'name' => "Fun",
        'help' => "Pub, Ice cream, ..",
    ),
    'RECUR' => array(
        'name' => "Recurring",
        'help' => "Monthly/yearly/.. recurring expenses",
    ),
    'ONE' => array(
        'name' => "One-time",
        'help' => "Furniture, electronics, ..",
    ),
    'PRES' => array(
        'name' => "Gifts",
        'help' => "",
    ),
    'CLOTH' => array(
        'name' => "Clothing",
        'help' => "",
    ),
    'MED' => array(
        'name' => "Medical",
        'help' => "",
    ),
    'TRANS' => array(
        'name' => "Transportation",
        'help' => "",
    ),
    'FINAN' => array(
        'name' => "Financial",
        'help' => "Interest, bank charges, ..",
    ),
    'IN' => array(
        'name' => "Income",
        'help' => "wages, ..",
        'default_positive_price' => true,
    ),
    'TAX' => array(
        'name' => "Taxes",
        'help' => "",
    ),
    'ACC' => array(
        'name' => "Accounting",
        'help' => "Non-real flows of money",
        'default_positive_price' => true,
    ),
    'ENDOW' => array(
        'name' => "Endowments",
        'help' => "Common account refill",
        'default_positive_price' => true,
    ),
);

// accounts
define('COMMON_ACCOUNT', "Common"); // account for common expenses
// list of accounts and their possible income/expense categories
$ACCOUNTS = array(
    "Common" => array(
        'LIFE',
        'FUN',
        'TRANS',
        'CLOTH',
        'RECUR',
        'ONE',
        'PRES',
        'MED',
        'FINAN',
        'ENDOW',
        'ACC',
    ),
    "Alice" => array(
        'FUN',
        'TRANS',
        'CLOTH',
        'RECUR',
        'ONE',
        'PRES',
        'MED',
        'IN',
        'FINAN',
        'TAX',
        'ENDOW',
        'ACC',
    ),
    "Bob" => array(
        'FUN',
        'TRANS',
        'CLOTH',
        'RECUR',
        'ONE',
        'PRES',
        'MED',
        'IN',
        'FINAN',
        'TAX',
        'ENDOW',
        'ACC',
    ),
);

define('CASH_METHOD_CODE', 'CASH'); // code of the cash payment method
define('DEFAULT_LIQUIDATION_METHOD', 'CARD'); // code of default method that is used to settle debt
// list of money reservoirs that want to be kept track of separately
$CASH_FLOW_LABELS = array(
    'CASH' => "Cash",
    'CARD' => "Card",
);
// payment methods corresponding to reservoirs of the previous setting
$PAYED_WITH_WHAT = array(
    'CASH' => "Cash",
    'CARD' => "Card",
);
// the relevant combinations of accounts and payment methods
$ACCOUNT_METHOD_COMBINATIONS = array(
    array('CARD', 'Common'),
    array('CASH', 'Alice'),
    array('CARD', 'Alice'),
    array('CASH', 'Bob'),
    array('CARD', 'Bob'),
);

// shorter names for brevity of the overviews
$SHORTER_NAMES = array(
    //"Alice Longname" => "Alice"
);
// shorter names for brevity of the overviews (should only be a few characters)
$VERY_SHORT_NAMES = array(
    "Common" => "Com.",
    "Alice" => "A.",
    "Bob" => "B.",
);

