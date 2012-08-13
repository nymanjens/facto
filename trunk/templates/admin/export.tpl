{include 'header.tpl'}

{include 'admin/menu.tpl'}

<div class="borderwrap_without_width" style="width: 200px;">
  <div class="frame_title">{t('Export')}</div>
  <table class="frame_cont" cellspacing="1">
	<tr>
	  <td><a href="{$root_url}admin/export/file" class="link">{t('Export database to file')}</a></td>
	</tr>
  </table>
</div>

{include 'footer.tpl'}
