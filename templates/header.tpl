<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
  <meta http-equiv="content-style-type" content="text/css">
  <meta http-equiv="content-language" content="en-gb">
  <meta http-equiv="imagetoolbar" content="no">
  <meta name="resource-type" content="document">
  <meta name="distribution" content="global">
  <meta name="copyright" content="2010-{date_format $.now "%Y"},  Jens Nyman">
  <meta name="keywords" content="family accounting tool facto">
  <title>{$page_title}</title>
  <link rel="stylesheet" type="text/css" href="{$root_url}scripts/style.css" />
  {foreach $include_style script}
      <link rel="stylesheet" type="text/css"
      href="{$root_url}{$script}" />
  {/foreach}
  {foreach $include_script script}
    <script language="JavaScript"
      type="text/javascript" src="{$root_url}{$script}">
    </script>
  {/foreach}
  <script language="JavaScript" type="text/javascript" src="{$root_url}scripts/jquery.js"></script>
  <script language="JavaScript" type="text/javascript" src="{$root_url}scripts/jquery.shortcuts.min.js"></script>
  <script language="JavaScript" type="text/javascript" src="{$root_url}scripts/jquery.fieldselection.js"></script>
  <script language="JavaScript" type="text/javascript">
    // configure and add shortcuts
    shortcuts = {
      'Shift+Alt+n': "accounting/input?returnto={self_url}",
      'Shift+Alt+e': "accounting/overview/everything",
      'Shift+Alt+a': "accounting/overview/everything",
      'Shift+Alt+i': "accounting/overview/income_expenses",
      'Shift+Alt+c': "accounting/overview/cash_flow",
      'Shift+Alt+l': "accounting/overview/liquidation",
      'Shift+Alt+v': "accounting/overview/liquidation",
      'Shift+Alt+w': "accounting/overview/endowments",
      'Shift+Alt+d': "accounting/overview/endowments",
      'Shift+Alt+s': "accounting/overview/summary",
      'Shift+Alt+o': "accounting/overview/summary",
    }
    for(var mask in shortcuts) {
      $.Shortcuts.add({
        type: 'down',
        mask: mask,
        handler: function() {
          window.location = "{$root_url}" + shortcuts[this.mask];
        }
      });
    }
    $.Shortcuts.start();
  </script>
  <link rel="SHORTCUT ICON" href="{$root_url}images/icon.png" />
  </head>
  <body>
    <center>
  {* title *}
  <div class="top_title_borderwrap">
    <div id="top_title">
      <a href="{$root_url}">
        <img src="{$root_url}images/top_title_logo.gif" style="vertical-align: top;" alt="Home" border="0"></a>
    </div>
    <div id="submenu">
    {foreach from=$top_links item=link}
      {if $link.link != $selected_link} {* regular link *}
        <div class="tts-top-{$link.orientation}-link"><a href="{$root_url}{$link.link}">{$link.label}</a></div>
      {else}
        <div class="tts-top-{$link.orientation}-link"><a href="{$root_url}{$link.link}" style="color: #353513;">
        {$link.label}</a></div>
      {/if}
    {/foreach}
    </div>
  </div>
