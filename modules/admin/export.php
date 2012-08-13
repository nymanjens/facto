<?php

// settings
$tables = array(TAG.'account_inputs', TAG.'logs', TAG.'users');

if( isset($_GET['SECRET_KEY']) && $_GET['SECRET_KEY'] ==  SECRET_KEY )
    echo @backup_tables($tables);
    
else if(isset($HTTP_REQUEST[2]) && $HTTP_REQUEST[2] == 'file') {
    header("Content-type: application/text");
    header("Content-Disposition: attachment; filename=backup.sql");
    // echo export
    echo @backup_tables($tables);
    
} else {
    // output
    $template = get_template("export");
    $dwoo->output($template, $view);
}



