<?php
/**
 * JForm functions
 * 
 * @author Jens Nyman (nymanjens.nj@gmail.com)
 */

/**
 * get requested url
 *
 * @return string
 */
function jform_self_url(){
	return $_SERVER['REQUEST_URI'];
}