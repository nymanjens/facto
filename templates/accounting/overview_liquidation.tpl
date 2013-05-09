{include 'header.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

{include 'overview_actions.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

{foreach $lists account}
  <div class="borderwrap_without_width full_width_overview">
    <div class="frame_title">{$account.title}</div>
    <table class="frame_cont data_table" cellspacing="1">
      <tr>
        <th>{t('Date')}</th>
        <th>{t('Beneficiary')}</th>
        <th>{t('Payed with/to')}</th>
        <th>{t('Category')}</th>
        <th>{t('Description')}</th>
        <th>{t('Flow')} ({$currency_symbol})</th>
        <th>{$account.totaltitle} ({$currency_symbol})</th>
        <th>&nbsp;</th>
      </tr>
      {foreach $account.list elem}
        <tr>
          <td>{$elem.time}</td>
          <td>{$elem.account_short}</td>
          <td>{$elem.payed_with_name}</td>
          <td>{$elem.category_name}</td>
          <td>{$elem.description}</td>
          <td>{$elem.price_str}</td>
          {if $.foreach.default.first}
            <td style="font-weight: bold;">{$elem.total}</td>
          {else}
            <td>{$elem.total}</td>
          {/if}
          <td class="buttons">{$elem.html_edit_delete}</td>
        </tr>
        {if $.foreach.default.last}
          {if $account.expand_link}
            <tr><td colspan="50" style="text-align: center;">
              <a href="{$account.expand_link}" class="link">...</a>
            </td></tr>
          {/if}
          <tr><td colspan="50" style="text-align: right;">
            <a href="{$root_url}accounting/liquidate/{$account.account1}/{$account.account2}/{$account.total}" class="link">{t('Liquidate')}</a>
          </td></tr>
        {/if}
      {else}
        <tr><td colspan="50">{t('Nothing present')}</td></tr>
      {/foreach}
    </table>
  </div>
  {* spacer *}
  <div style="margin-top: 30px"></div>
  
{/foreach}

{include 'footer.tpl'}

