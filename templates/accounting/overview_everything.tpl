{include 'header.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

{include 'overview_actions.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

<div class="borderwrap_without_width full_width_overview">
  <div class="frame_title">{t('All Entries')}</div>
  <table class="frame_cont data_table" cellspacing="1">
    <tr>
      <th>{t('Date')}</th>
      <th>{t('Issuer')}</th>
      <th>{t('Beneficiary')}</th>
      <th>{t('Payed with/to')}</th>
      <th>{t('Category')}</th>
      <th>{t('Description')}</th>
      <th>{t('Flow')} ({$currency_symbol})</th>
      <th>&nbsp;</th>
    </tr>
    {foreach $list elem}
      <tr>
        <td>{$elem.time}</td>
        <td>{$elem.issuer_name}</td>
        <td>{$elem.account}</td>
        <td>{$elem.payed_with_name}</td>
        <td>{$elem.category_name}</td>
        <td>{$elem.description}</td>
        <td>{$elem.price_str}</td>
        <td class="buttons">{$elem.html_edit_delete}</td>
      </tr>
    {else}
      <tr><td colspan="50">{t('No transactions registered yet')}</td></tr>
    {/foreach}
    {if $expand_link}
      <tr><td colspan="50" style="text-align: center;">
        <a href="{$expand_link}" class="link">...</a>
      </td></tr>
    {/if}
  </table>
</div>

{include 'footer.tpl'}

