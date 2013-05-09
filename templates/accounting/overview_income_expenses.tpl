{include 'header.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

{include 'overview_actions.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

<table class="overview_pane"><tr><td>
  {foreach $lists account}
    <div class="tile">
      <div class="borderwrap_without_width">
        <div class="frame_title">{$account.title}</div>
        <table class="frame_cont data_table" cellspacing="1">
          <tr>
            <th>{t('Date')}</th>
            <th>{t('Category')}</th>
            <th>{t('Description')}</th>
            <th>{t('Flow')} ({$currency_symbol})</th>
            <th>&nbsp;</th>
          </tr>
          {foreach $account.list elem}
            <tr>
              <td>{$elem.time}</td>
              <td>{$elem.category_name}</td>
              <td>{$elem.description}</td>
              <td>{$elem.price_str}</td>
              <td class="buttons">{$elem.html_edit_delete}</td>
            </tr>
          {else}
            <tr><td colspan="50">{t('No transactions registered yet')}</td></tr>
          {/foreach}
          {if $account.expand_link}
            <tr><td colspan="50" style="text-align: center;">
              <a href="{$account.expand_link}" class="link">...</a>
            </td></tr>
          {/if}            
        </table>
      </div>
    </div>
  {/foreach}
</td></tr></table>

{include 'footer.tpl'}

