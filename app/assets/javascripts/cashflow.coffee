$(document).ready(() ->
  $('button[ajax-href]').click () ->
    $button = $(this)
    l = Ladda.create(this)
    l.start()
    $.post($button.attr('ajax-href')) # URL
      .always(() -> l.stop())
      .done(() ->
        $confirmedContent =
            $button
              .parent(".balance-check-confirm-group")
              .find(".content-when-confirmed")
        $confirmedContent.removeClass("hidden")
        $button.addClass("hidden")
      )
)
