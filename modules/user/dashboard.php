<?php
/**
 * user - dashboard
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

// for link highlight
$view->assign('selected_link', 'user/dashboard');

// user must be logged in to view this board
if(!$user->isLoggedIn())
	redirect(ROOT_URL);

/************ names form ************/
$form_names = new JForm( array(
	'submit_label' => t("Change"),
	'inputs' => array(
		'login_name' => array(
			'label' => t('Login Name'),
			'required' => "true",
			'value' => $user->getSession('login_name'),
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
			'maxlength' => MAX_LOGIN_NAME_SIZE,
			
			'disabled' => 'disabled',
		),
		'name' => array(
			'label' => t('Name'),
			'required' => "true",
			'value' => $user->name,
			'filters' => new DefaultTextFilter(),
			'postfilters' => new SqlPostFilter(),
			'maxlength' => MAX_NAME_SIZE,
			
			'disabled' => 'disabled',
		),
	)
));

/************ password form ************/
$form_password = new JForm( array(
	'submit_label' => t("Change"),
	'inputs' => array(
		'password_old' => array(
			'label' => t("Old Password"),
			'type' => "password",
			'required' => "true",
			'validators' => new Md5EqualValidator(
				$user->getSession('password')),
			'postfilters' => new Md5PostFilter(),
			'autocomplete' => "off",
		),
		'password_new' => array(
			'label' => t("New Password"),
			'type' => "password",
			'required' => "true",
			'validators' => new PasswordValidator(),
			'postfilters' => new Md5PostFilter(),
			'autocomplete' => "off",
		),
	)
));
if($form_password->isSubmittedAndValid() ) {
	// get values
	$values = $form_password->getValues();
	$password_old = $values['password_old'];
	$password = $values['password_new'];
	// validate
	if($password_old == $password)
		$form_password->data['inputs']['password_old']['error'] =
			'No changes were made';
	else {
		// valid
		$q = "UPDATE ".TAG."users SET password='$password'
			WHERE id=$user->id";
		query($q);
		$user->setSession('password', $password);
		// set instant message
		$user->setInstantMessage('message', t("Password updated"));
		redirect(self_url());
	}
}

/************ output ************/
if($user->hasInstantMessage('message'))
	$view->assign('instant_message', $user->getInstantMessage('message'));
$view->assign('form_names', $form_names->getHtml());
$view->assign('form_password', $form_password->getHtml());
$template = get_template("dashboard");
$dwoo->output($template, $view);

