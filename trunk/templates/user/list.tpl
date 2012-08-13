{include 'header.tpl'}

{include 'admin/menu.tpl'}

<div class="borderwrap">
  <div class="frame_title">{t('View Users')}</div>
  <table class="frame_cont data_table" cellspacing="1">
  	<tr>
	  <th>{t('Login Name')}</th><th>{t('Name')}</th><th>{t('Last Visit')}</th>
	</tr>
	{foreach $users user}
	<tr>
	  <td>{$user.login_name}</td><td>{$user.name}</td>
	  <td>{$user.last_visit}</td>
	</tr>
	{/foreach}
  </table>
</div>

{include 'footer.tpl'}
