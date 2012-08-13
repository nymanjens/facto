{include 'header.tpl'}

<h1>{t('Dashboard')}</h1>

{if $instant_message}
	<div style="font-weight: bold; margin-top: 20px; text-align: center;">
		{$instant_message}</div>
{/if}

{* spacer *}
<div style="margin-top: 20px"></div>

{************ FORM NAMES ************}
{$form_names.begin}
<div class="borderwrap">
  <div class="frame_title">{t('Names')}</div>
  <table class="frame_cont" cellspacing="1"><tr><td style="text-align: center;">
    <table align="center" class="dashboard">
	{foreach $form_names.inputs input}
	  {if $input.error}<tr><td></td><td class="form_error">
	    {$input.error}</td></tr>{/if}
	  <tr>
	    <td class="left">{$input.label}:</td>
		<td>{$input.input}</td>
	  </tr>
	{/foreach}
	{* <tr><td></td><td>{$form_names.submit.input}</td></tr> *}
	</table>
  </td></tr></table>
</div>
{$form_names.end}

{* spacer *}
<div style="margin-top: 20px"></div>

{************ FORM PASSWORD ************}
{$form_password.begin}
<div class="borderwrap">
  <div class="frame_title">{t('Edit Password')}</div>
  <table class="frame_cont" cellspacing="1"><tr><td style="text-align: center;">
    <table align="center" class="dashboard">
	{foreach $form_password.inputs input}
	  {if $input.error}<tr><td></td><td class="form_error">
	    {$input.error}</td></tr>{/if}
	  <tr>
	    <td class="left">{$input.label}:</td>
		<td>{$input.input}</td>
	  </tr>
	{/foreach}
	<tr><td></td><td>{$form_password.submit.input}</td></tr>
	</table>
  </td></tr></table>
</div>
{$form_password.end}

{include 'footer.tpl'}

