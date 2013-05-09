{include 'header.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

{include 'overview_actions.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

{foreach $lists account}
  {foreach $account method}
    <div class="borderwrap_without_width full_width_overview">
      <div class="frame_title">{$method.title}</div>
      <table class="frame_cont data_table" cellspacing="1">
        <tr>
          <th>{t('Date')}</th>
          <th>{t('Beneficiary')}</th>
          <th>{t('Category')}</th>
          <th>{t('Description')}</th>
          <th>{t('Flow')} ({$currency_symbol})</th>
          <th>{t('Balance')} ({$currency_symbol})</th>
          <th>&nbsp;</th>
        </tr>
        {foreach $method.list elem}
          {if $elem.category != '[BALANCE_SET]'}
            <tr>
              <td>{$elem.time}</td>
              <td>{$elem.account}</td>
              <td>{$elem.category_name}</td>
              <td>{$elem.description}</td>
              <td>{$elem.price_str}</td>
              <td>
                {if $.foreach.default.first}
                  <b>{$elem.balance}</b>
                {else}
                  {$elem.balance}
                {/if}
                {if $elem.verified_balance}
                  <img src="{$root_url}images/verified.png" style="margin: -4px 0 0 8px;" />
                {/if}
              </td>
              <td class="buttons">{$elem.html_edit_delete}</td>
            </tr>
          {else}
            <tr>
              <td>{$elem.time}</td>
              <td colspan="3"><b>{t('Balance correction')}:</b></td>
              <td></td>
              <td>{$elem.balance}</td>
              <td class="buttons">{$elem.html_edit_delete}</td>
            </tr>
            
          {/if}
        {/foreach}
        {if $method.expand_link}
          <tr><td colspan="50" style="text-align: center;">
            <a href="{$method.expand_link}" class="link">...</a>
          </td></tr>
        {/if}
        <tr><td colspan="50" style="text-align: right;">
          <a href="{$root_url}accounting/set_balance/{$method.method}/{$method.account}/{$method.default_balance}/?returnto={self_url}" class="link">{t('Set balance')}</a>
        </td></tr>
      </table>
    </div>
    {* spacer *}
    <div style="margin-top: 8px"></div>
  {/foreach}
  {* spacer *}
  <div style="margin-top: 25px"></div>
{/foreach}
{include 'footer.tpl'}

