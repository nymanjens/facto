{include 'header.tpl'}

{include 'admin/menu.tpl'}

{if $instant_message}
	<div style="font-weight: bold; margin-top: 20px; text-align: center;">
		{$instant_message}</div>
{/if}

{* spacer *}
<div style="margin-top: 20px"></div>

{$form.begin}
<div class="borderwrap">
  <div class="frame_title">{t('Register new user')}</div>
  <table class="frame_cont" cellspacing="1"><tr><td style="text-align: center;">
    <table align="center">
	{foreach $form.inputs input}
	  {if $input.error}<tr><td></td><td class="form_error">
	    {$input.error}</td></tr>{/if}
	  <tr>
	    <td style="text-align: right;">{$input.label}:</td>
		<td>{$input.input}</td>
	  </tr>
	{/foreach}
	<tr><td></td><td>{$form.submit.input}</td></tr>
	</table>
  </td></tr></table>
</div>
{$form.end}

{include 'footer.tpl'}
