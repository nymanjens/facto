{if $instant_message}
	<div style="font-weight: bold; margin-top: 20px; text-align: center;">
		{$instant_message}</div>
{/if}

<table class="overview_pane"><tr><td>
  <div class="borderwrap_without_width overview_actions" >
    <div class="frame_title">{t('Actions')}</div>
    <table class="frame_cont" cellspacing="1">
      <tr>
        {if !$show_all_accounts && !$disable_num_limit}
          {if !$actions_hide_show_all_accounts}
            <td><a href="{$self_path}?show_all_accounts=1" class="link">{$show_all_accounts_label}</a></td>
          {/if}
          {if !$actions_hide_num_limit}
            <td><a href="{$self_path}?disable_num_limit=1" class="link">{$disable_num_limit_label}</a></td>
          {/if}
        {elseif $show_all_accounts && !$disable_num_limit}
          {if !$actions_hide_show_all_accounts}
            <td><a href="{$self_path}" class="link">{$show_all_accounts_label}</a></td>
          {/if}
          {if !$actions_hide_num_limit}
            <td><a href="{$self_path}?show_all_accounts=1&disable_num_limit=1" class="link">{$disable_num_limit_label}</a></td>
          {/if}
        {elseif !$show_all_accounts && $disable_num_limit}
          {if !$actions_hide_show_all_accounts}
            <td><a href="{$self_path}?show_all_accounts=1&disable_num_limit=1" class="link">{$show_all_accounts_label}</a></td>
          {/if}
          <td><a href="{$self_path}" class="link">{$disable_num_limit_label}</a></td>
        {elseif $show_all_accounts && $disable_num_limit}
          {if !$actions_hide_show_all_accounts}
            <td><a href="{$self_path}?disable_num_limit=1" class="link">{$show_all_accounts_label}</a></td>
          {/if}
          <td><a href="{$self_path}?show_all_accounts=1" class="link">{$disable_num_limit_label}</a></td>
        {/if}
      </tr>
      {if !$actions_hide_new_entry}
        <tr>
          <td colspan="20"><a href="{$root_url}accounting/input?returnto={self_url}" class="link"><img src="{$root_url}images/new.png" />
            <u>{t('N</u>ew entry')}</a></td>
        </tr>
      {/if}
      {if $actions_show_withdrawal_button}
        <tr>
          <td colspan="20"><a href="{$root_url}accounting/withdrawal?returnto={self_url}" class="link"><img src="{$root_url}images/new.png" />
            {t('Insert withdrawal')}</a></td>
        </tr>
      {/if}
      {if $actions_show_endowment_button}
        <tr>
          <td colspan="20"><a href="{$root_url}accounting/endowment?returnto={self_url}" class="link"><img src="{$root_url}images/new.png" />
            {t('Make endowment')}</a></td>
        </tr>
      {/if}
    </table>
  </div>
</td></tr></table>
