'use strict';

$(document).ready(() => {

  // show next hidden entries
	$('.btn-show-next-hidden-entries').click(function(){
		const thisRow = $(this).closest('tr')
		thisRow.siblings(".next-hidden-entries").removeClass("hidden")
		thisRow.hide()
	})

  // toggle other accounts
	$('.btn-toggle-other-accounts').click(function(){
		$('.other-accounts').toggleClass("hidden")
	})

  // ToC
  function reloadToc() {
    function getTocEntry(item, entryClasses, iconClasses) {
      const id = item.id
      const title = $(item).find('.toc-title').first().html().trim()
      return `<a href="#${id}" class="${entryClasses}"><i class="fa fa-fw ${iconClasses}"></i> ${title}</a>`
    }

    let tocContent = ''
    $('.add-toc-level-1').each((_, item1) => {
      if(!$(item1).hasClass('hidden')) {
        tocContent += getTocEntry(item1, "toc-level-1", "fa-angle-right")
        $(item1).find('.add-toc-level-2').each((_, item2) => {
          tocContent += getTocEntry(item2, "toc-level-2", "fa-caret-right")
        })
      }
    })

    $('.toc-placeholder').html(tocContent)
  }
  reloadToc()
  $('.btn-toggle-other-accounts').click(reloadToc)

  // Key bindings
  const shortcuts = {
    'shift+alt+e': "menu-link-everything",
    'shift+alt+a': "menu-link-everything",
    'shift+alt+c': "menu-link-cashflow",
    'shift+alt+l': "menu-link-liquidation",
    'shift+alt+v': "menu-link-liquidation",
    'shift+alt+d': "menu-link-endowments",
    'shift+alt+s': "menu-link-summary",
    'shift+alt+t': "menu-link-templates",
    'shift+alt+j': "menu-link-templates",
    'shift+alt+n': "menu-link-newtransgroup",
  }
  $.each(shortcuts, (key_combo, link_id) => {
    Mousetrap.bind(key_combo, (e) => {
      if (e.preventDefault) {
        e.preventDefault();
      }
      document.getElementById(link_id).click()
    });
  })
  Mousetrap.bind("shift+alt+f", (e) => {
    if (e.preventDefault) {
      e.preventDefault();
    }
    document.getElementById("search-box").focus()
  });
})
