<?php
/**
 * user - register
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// for link highlight
$view->assign('selected_link', 'user/users');

// user must be logged in to view this board
if(!$user->isLoggedIn())
	redirect(ROOT_URL);

// register form
$form = new JForm( array(
	'focus' => "", // focus first element
	'submit_label' => t("Register"),
	'inputs' => array(
		'login_name' => array(
			'label' => t("Login Name"),
			'required' => "true",
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
			'maxlength' => MAX_LOGIN_NAME_SIZE,
		),
		'password' => array(
			'label' => t("Password"),
			'type' => "password",
			'required' => "true",
			'validators' => new PasswordValidator(),
			'postfilters' => new Md5PostFilter(),
			'autocomplete' => "off",
		),
		'name' => array(
			'label' => t("Name"),
			'required' => "true",
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
			'maxlength' => MAX_NAME_SIZE,
		),
	)
));

// form is submitted
if( $form->isSubmittedAndValid() ) {
	// get values
	$values = $form->getValues();
	$login_name = $values['login_name'];
	$password = $values['password'];
	$name = $values['name'];
	// validate
	$valid = true;
	if(mysql_num_rows(query("SELECT * FROM ".TAG."users
			WHERE lower(login_name)='".strtolower($login_name)."'"))
			!= 0) {
		$valid = false;
		$form->data['inputs']['login_name']['error'] =
			'This login name is already taken';
	}
	if(mysql_num_rows(query("SELECT * FROM ".TAG."users
			WHERE lower(name)='".strtolower($name)."'")) != 0) {
		$valid = false;
		$form->data['inputs']['name']['error'] =
			'This name is already taken';
	}
	if($valid) {
		// valid registration
		$q = "INSERT INTO ".TAG."users (login_name, password, name)
			VALUES ('$login_name', '$password', '$name')";
		query($q);
		// set instant message for dashboard to display
		$user->setInstantMessage('message', t("Registration of new user complete"));
		redirect(self_url());
	}
}

// output
if($user->hasInstantMessage('message'))
	$view->assign('instant_message', $user->getInstantMessage('message'));
$view->assign('form', $form->getHtml());
$template = get_template("register");
$dwoo->output($template, $view);

