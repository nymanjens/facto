{include 'header.tpl'}

{include 'admin/menu.tpl'}

<div class="borderwrap">
  <div class="frame_title">{t('Logs')}</div>
  <table class="frame_cont data_table" cellspacing="1">
  	<tr>
	  <th style="width: 70px;">{t('Time')}</th><th>{t('Issuer')}</th><th>SQL</th>
	</tr>
	{foreach $logs log}
	<tr>
	  <td>{$log.time}</td>
	  <td>{$log.user}</td>
	  <td>{$log.sql}</td>
	</tr>
	{/foreach}
  </table>
</div>

{include 'footer.tpl'}
