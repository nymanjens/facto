{include 'header.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

{include 'overview_actions.tpl'}

{* spacer *}
<div style="margin-top: 10px"></div>

{foreach $lists account name="l0"}
  <div class="borderwrap_without_width full_width_overview">
    <div class="frame_title">{$account.title}</div>
    <table class="frame_cont" cellspacing="1">
      <tr>
        <th>&nbsp;</th>
        {foreach $account.years year months}
          {if !$.foreach.default.last && !$show_all_years && $focus_year != $year}
            <th>{$year}</th>
          {else}
            <th colspan="{count_and_add_1($months)}">{$year}</th>
          {/if}
        {/foreach}
      </tr>
      <tr>
        <th>{t('Category')}</th>
        {foreach $account.years year months name="months_per_y"}
          {if $.foreach.months_per_y.last || $show_all_years || $focus_year == $year}
            {foreach $months month}
              <th>{$month}</th>
            {/foreach}
          {/if}
          <th>{t('Avg')}</th>
        {/foreach}
      </tr>
      {foreach $account.matrix category data_per_y}
        <tr>
          <td>{$CATEGORIES.$category.name}</td>
          {foreach $data_per_y year data name="forloop_per_y"}
            {if $.foreach.forloop_per_y.last || $show_all_years || $focus_year == $year}
              {foreach $data value}
                {if !$value.totexp}
                  <td style="color: #BBBBBB;">0.00</td>
                {else}
                  <td class="summary">
                    {$value.totexp_str}
                    <div class=" help_text">
                      <ul>
                      {foreach $value.elems elem}
                        <li><a href="{$elem.edit_link}">{$elem.description}</a>: {$currency_symbol} {$elem.price_str}</li>
                      {/foreach}
                      </ul>
                    </div>
                  </td>
                {/if}
              {/foreach}
              <td class="average">{$account.averages.$year.$category}</td>
            {else}
              <td class="average averagelink">
                <a href="{$self_path}?show_all_accounts={$disable_num_limit}&disable_num_limit={$disable_num_limit}&focus_year={$year}">
                  {$account.averages.$year.$category}
                </a>
              </td>
            {/if}
        {/foreach}
        </tr>
      {else}
      <tr><td>{t('No transactions registered yet')}</td></tr>
      {/foreach}
      {if $account.totals}
        <tr class="total">
          <td>{t('Total')}<span style="font-weight: normal; font-size: 9px;"> ({t('without acc.')})</span></td>
          {foreach $account.years year months}
            {if $.foreach.default.last || $show_all_years || $focus_year == $year}
              {foreach $months month}
                <td>{$account.totals.$year.$month}</td>
              {/foreach}
            {/if}
            <td class="average">{$account.averages.$year.total}</td>
          {/foreach}
        </tr>
      {/if}
    </table>
  </div>
  {* spacer *}
  <div style="margin-top: 30px"></div>
  
{/foreach}

{include 'footer.tpl'}

