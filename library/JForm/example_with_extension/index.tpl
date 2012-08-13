<script type="text/javascript" src="jquery.js"></script>
<style>
  * {
    margin: 2px;
	font-family: verdana;
	font-size: 13px;
  }
  div.error{
    color: red;
  }
  #form{
	width: 700px;
  }
  #form div.left, #form div.right{
	padding: 0px;
	margin: 3px;
	float: left;
  }
  #form div.left{
    width: 120px;
	text-align: right;
  }
  #form div.right{
	width: 450px;
  }
  #form div.newline{
	clear: both;
  }
</style>
<br />
<hr />
<p>Fill in this form please:</p>
<div id="form">
<a name="p" style="height:0;margin:0;padding:0;"></a>
{$form.begin}
{foreach from=$form.inputs item=elem}
{if $elem.error}<div class="left"></div><div class="right error">{$elem.error}</div><div class="newline"></div>{/if}
<div class="left">{$elem.label}:</div><div class="right">{$elem.input}</div><div class="newline"></div>
{/foreach}
<div class="left">{$form.submit.input}</div><div class="newline"></div>
{$form.end}
</div>
<hr />
<br />
<br />
<p style="font-size: 11px; text-align: center;">&copy; Jens Nyman</p>
