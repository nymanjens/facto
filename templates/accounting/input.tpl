{include 'header.tpl'}

{* spacer *}
<div style="margin-top: 20px"></div>

{$form.begin}
<div class="borderwrap">
  <div class="frame_title">{$frame_title}</div>
  <table class="frame_cont" cellspacing="1"><tr><td style="text-align: center;">
    <table align="center">
      {foreach $form.inputs inputname input}
        {if $input.error}<tr><td></td><td class="form_error">
          {$input.error}</td></tr>{/if}
        <tr>
          <td class="left">{$input.label}:</td>
        <td>{if $inputname == 'price'}{$currency_symbol}{/if} {$input.input}</td>
        </tr>
      {/foreach}
      <tr><td></td><td>{$form.submit.input}</td></tr>
    </table>
  </td></tr></table>
</div>
{$form.end}

{include 'footer.tpl'}

