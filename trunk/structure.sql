-- 
-- Basic table structure of facto with one administrator account
--
-- @author Jens Nyman (nymanjens.nj@gmail.com)
-- 

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

CREATE TABLE IF NOT EXISTS `facto_account_inputs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `issuer` int(11) NOT NULL,
  `account` text NOT NULL,
  `category` text NOT NULL,
  `payed_with_what` text NOT NULL,
  `payed_with_whose` text NOT NULL,
  `description` text NOT NULL,
  `price` float NOT NULL,
  `timestamp` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `issuer` (`issuer`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

CREATE TABLE IF NOT EXISTS `facto_logs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `timestamp` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `sql` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

CREATE TABLE IF NOT EXISTS `facto_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `login_name` text NOT NULL,
  `password` text NOT NULL,
  `name` text NOT NULL,
  `last_visit` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=27 ;

INSERT INTO `facto_users` (`id`, `login_name`, `password`, `name`, `last_visit`) VALUES
(1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 'Admin', 0);


