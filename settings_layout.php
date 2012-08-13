<?php
/**
 * layout settings
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

$LAYOUT_TOP_LINKS_LOGGEDOUT = array(
	array(
		'link' => "user/login",
		'orientation' => "left",
		'label' => t("Login"),
	),
);

$LAYOUT_TOP_LINKS_LOGGEDIN = array(
	array(
		'link' => "accounting/overview/everything",
		'orientation' => "left",
		'label' => t("<u>E</u>verything"),
	),
	array(
		'link' => "accounting/overview/income_expenses",
		'orientation' => "left",
		'label' => t("<u>I</u>ncome/Expenses"),
	),
	array(
		'link' => "accounting/overview/cash_flow",
		'orientation' => "left",
		'label' => t("<u>C</u>ash flow"),
	),
	array(
		'link' => "accounting/overview/liquidation",
		'orientation' => "left",
		'label' => t("<u>L</u>iquidation"),
	),
	array(
		'link' => "accounting/overview/endowments",
		'orientation' => "left",
		'label' => t("Endo<u>w</u>ments"),
	),
	array(
		'link' => "accounting/overview/summary",
		'orientation' => "left",
		'label' => t("<u>S</u>ummary"),
	),
	array(
		'link' => "user/logout",
		'orientation' => "right",
		'label' => t("Logout <name>"),
	),
	array(
		'link' => "user/dashboard",
		'orientation' => "right",
		'label' => t("Dashboard"),
	),
	array(
		'link' => "user/users",
		'orientation' => "right",
		'label' => t("Admin"),
	),
);

