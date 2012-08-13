<?php
/**
 * specific functions for this site
 *
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/**
 * looks id up in database and returns username (not login_name)
 * 
 * @param int $id
 * @return string
 */
function get_username_from_id($id) {
    if(!numeric($id))
        die("get_username_from_id(): Fatal Error: id isn't numeric");
    $res = query("SELECT name FROM ".TAG."users WHERE id=$id");
    if(mysql_num_rows($res) == 0)
        return "";
    return fetch_var($res);
}

/**
 * get initialized Dwoo template
 * 
 * @param string $filename
 * @return Dwoo_Template_File
 */
function get_template($filename) {
    if(strpos($filename, "/") === false)
        $filename = MODULE . "/" . $filename;
    if(file_get_extension($filename) == "")
        $filename = $filename . ".tpl";
    $template = new Dwoo_Template_File($filename);
    $template->setIncludePath(array("templates", "templates/" . MODULE,
        "."));
    return $template;
}

/**
 * Query with log
 * 
 * @param string $query
 * @return mysql result
 */
function query_with_log($query) {
    global $user;
    $res = query($query);
    $q = sprintf("INSERT INTO ".TAG."logs (timestamp, user_id, `sql`) VALUES (%d, %d, '%s')",
        time(), $user->id, mysql_real_escape_string($query));
    query($q);
    return $res;
}


/**
 * shorten names based on $SHORTER_NAMES
 */
function shortname($name, $very_short=False) {
    global $SHORTER_NAMES, $VERY_SHORT_NAMES;
    $dict = $very_short ? $VERY_SHORT_NAMES : $SHORTER_NAMES;
    if(isset($dict[$name]))
        return $dict[$name];
    return $name;
}





