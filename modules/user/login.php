<?php
/**
 * user - login
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// for link highlight
$view->assign('selected_link', 'user/login');

// user may not be logged in
if($user->isLoggedIn())
	redirect(ROOT_URL);

// login form
$form = new JForm( array(
	'focus' => "", // focus first element
	'submit_label' => t("Login"),
	'inputs' => array(
		TAG.'login_name' => array(
			'label' => t("Login Name"),
			'required' => "true",
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
			'maxlength' => MAX_LOGIN_NAME_SIZE,
		),
		TAG.'password' => array(
			'label' => t("Password"),
			'type' => "password",
			'required' => "true",
			'postfilters' => new Md5PostFilter(),
		),
	)
));

// set known default values
$name = "";
if($user->getSession('login_name') != "")
	$name = $user->getSession('login_name');
else
	$name = $user->name;
$form->setDefaultValues(array(
	TAG.'login_name' => strtolower($name),
));

// form is submitted
if( $form->isSubmittedAndValid() ) {
	$values = $form->getValues();
	$login_name = $values[TAG.'login_name'];
	$password = $values[TAG.'password'];
	$q = "SELECT * FROM ".TAG."users WHERE password='$password'
		AND lower(login_name)='".strtolower($login_name)."'";
	$res = query($q);
	if(mysql_num_rows($res) == 1){
		// valid login attempt
		$o = mysql_fetch_object($res);
		$user->setSession('password', $o->password);
		$user->setSession('login_name', $o->login_name);
		$user->setSession('name', $o->name);
		redirect(ROOT_URL);
	} else {
		// invalid login attempt: notify and save information
		// notify
		if(mysql_num_rows(query("SELECT * FROM ".TAG."users WHERE
				lower(login_name)='".strtolower($login_name)."'")) == 0)
			$form->data['inputs'][TAG.'login_name']['error'] =
				"This login name doesn't exist";
		else
			$form->data['inputs'][TAG.'password']['error'] =
				"Password is incorrect";
		// save information
		$user->setSession('name', $login_name);
		$user->setSession('login_name', $login_name);
	}
}

// output
$view->assign('form', $form->getHtml());
$template = get_template("login");
$dwoo->output($template, $view);

