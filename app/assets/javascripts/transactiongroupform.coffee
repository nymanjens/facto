### utility functions ###
parseMoneyAsFloatToCents = (moneyAsFloatString) ->
  parsed = parseFloat(moneyAsFloatString)
  if isNaN(parsed)
    parsed = 0
  Math.round(parsed * 100)

centsToFloatString = (inCents) ->
  afterDot = "" + Math.abs(inCents%100)
  if afterDot.length < 2
    afterDot = "0" + afterDot
  beforeDot = Math.round(Math.abs(inCents / 100))
  sign = ""
  sign = "-" if(inCents < 0)
  "#{sign}#{beforeDot}.#{afterDot}"

### update total functions ###
updateAllTotalState = ($thisFormContainer) ->
  getTotalInCentsFromInputs = () ->
    totalInCents = 0
    $(".flow-as-float").each(() ->
      totalInCents += parseMoneyAsFloatToCents(this.value)
    )
    totalInCents

  isZeroSumForm = () ->
    $("input:radio[name=zeroSum]:checked").val() == "true"

  fixTotalZeroIfNotLast = (totalInCents) ->
    $lastContainer = $(".transaction-holder").last()
    isLast = $lastContainer.is($thisFormContainer)
    numForms = $(".transaction-holder").length
    if isZeroSumForm() and not isLast and numForms > 1
      lastValue = parseMoneyAsFloatToCents(($lastContainer.find(".flow-as-float").val()))
      newLastValue = lastValue - totalInCents
      $lastContainer.find(".flow-as-float").val(centsToFloatString(newLastValue))
      0
    else
      totalInCents

  updateTotal = (totalInCents) ->
    $(".total-transaction-flow").html(centsToFloatString(totalInCents))

  updateTotalColor = (totalInCents) ->
    zeroSum = isZeroSumForm()
    $(".total-flow-text").toggleClass("nonzero-warning", zeroSum && totalInCents != 0)

  totalInCents = getTotalInCentsFromInputs()
  totalInCents = fixTotalZeroIfNotLast(totalInCents)
  updateTotal(totalInCents)
  updateTotalColor(totalInCents)

$(document).ready(() ->
  ### constants ###
  ROOT_FORM_CONTAINER = $('#transaction-holder-0')

  ### make add-transaction window have the same height as all other windows ###
  $('.add-transaction-button-holder .panel-body').height($('#transaction-holder-0 .panel-body').height())

  ### define TransactionNumGenerator ###
  class TransactionNumGenerator
    highestTransactionNum: 0
    constructor: () ->
      while($("#transaction-holder-#{@highestTransactionNum+1}").length)
        @highestTransactionNum++
    getNext: () ->
      return ++@highestTransactionNum
  transactionNumGenerator = new TransactionNumGenerator()

  ### new transaction behaviour ###
  $(".add-transaction-button").click(() ->
    transactionNum = transactionNumGenerator.getNext()
    newForm = ROOT_FORM_CONTAINER.clone()

    # bugfix in clone(): manually copying select selection and textarea content
    newForm.find('select').each((index, item) -> $(item).val(ROOT_FORM_CONTAINER.find('select').eq(index).val()))
    newForm.find('textarea').each((index, item) -> $(item).val(ROOT_FORM_CONTAINER.find('textarea').eq(index).val()))

    # update names, ids and title to the correct transactionNum
    newForm.find("[id]").add(newForm).each(() ->
      @id = @id.replace("0", transactionNum)
    )
    newForm.find("[name]").each(() ->
      $(this).attr('name', $(this).attr('name').replace("[0]", "[#{transactionNum}]"))
    )
    newForm.find(".panel-heading-title").html("Transaction #{transactionNum+1}")

    # reset price
    newForm.find(".form-input-money").val("0.00")

    # unhide rm-button
    newForm.find(".rm-transaction-button-holder").removeClass("hidden")

    # add to pane and register listeners
    newForm.appendTo("#extra-transactions")
    addTransactionSpecificEventListeners(newForm)

    # update total
    updateAllTotalState(null)
  )

  ### transaction-specific events ###
  addTransactionSpecificEventListeners = (formContainer) ->
    $formContainer = $(formContainer)
    ### remove transaction button behaviour ###
    $formContainer.find(".rm-transaction-button").click(() ->
      $formContainer.remove()
    )

    ### enforce bind-until-change ###
    $formContainer.find(".bind-until-change").each(() ->
      # get boundSources
      getBoundSources = (boundElem) ->
        elemName = boundElem.attr('name')
        sources = $()
        $.each(boundElem.attr('class').split(/\s+/), (index, clazz) ->
          if(clazz.startsWith("bind-to-formfield-"))
            fieldSimpleName = stripPrefix(clazz, "bind-to-formfield-")
            sourceElemName = elemName.split('.')[0] + "." + fieldSimpleName
            sources = sources.add($formContainer.find("[name='#{sourceElemName}']"))

          else if (clazz == "bind-to-root-form")
            isRootElem = elemName.contains("[0]")
            if(!isRootElem)
              sourceElemName = elemName.replace(/\[\d\]/, "[0]")
              sources = sources.add(ROOT_FORM_CONTAINER.find("[name='#{sourceElemName}']"))
        )
        return sources

      boundElem = $(this)
      boundSources = getBoundSources(boundElem)

      if(boundSources.length > 0)
        # update bounded-state
        updateBoundedState = () ->
          equalToAnySource = false
          boundSources.each(() ->
            if($(this).val() == boundElem.val())
              equalToAnySource = true
          )
          boundElem.toggleClass("bound-until-change", equalToAnySource)

        updateBoundedState()
        boundElem.keyup(() -> setTimeout(updateBoundedState))
        boundElem.change(() -> setTimeout(updateBoundedState))
        if(boundElem.attr("type") == "text" || boundElem[0].tagName == "TEXTAREA")
          boundSources.keydown((e) ->
            sourceElem = $(this)
            oldSourceElemValue = sourceElem.val()
            boundElemValue = boundElem.val()
            setTimeout(() ->
              newSourceElemValue = sourceElem.val()
              if(boundElem.hasClass("bound-until-change") && oldSourceElemValue == boundElemValue)
                boundElem.val(newSourceElemValue)

              #updateBoundedState() # might be better that bound-until-change does not get picked up be changing source
            )
            if(boundElem.hasClass("bound-until-change"))
              boundElem.trigger(e) # trigger event for handling transitive binds(should schedule its timeout after this one)

          )
        else # non-text (e.g. dropdown): bind-transitiviy is not supported, which is a major simplification
          boundSources.change(() ->
            if(boundElem.hasClass("bound-until-change"))
              sourceElem = $(this)
              boundElem.val(sourceElem.val())
              boundElem.change() # allow e.g. updateCategoires to react

          )
          boundSources.keydown(() -> sourceElem = $(this); setTimeout(() -> sourceElem.change()))
    )

    ### filter categories, based on current beneficiaryAccount ###
    $beneficiaryAccountSelect = $formContainer.find("select[id$=_beneficiaryAccountCode]")
    $categorySelect = $formContainer.find("select[id$=_categoryCode]")
    updateCategories = () ->
      beneficiaryAccountCode = $beneficiaryAccountSelect.val()
      # update option's hidden state
      $formContainer.find("option[accounts]").each(() ->
        $option = $(this)
        accounts = $option.attr("accounts").split(' ')
        $option.toggleClass("hidden", beneficiaryAccountCode not in accounts)
      )
      # make sure the current value is not hidden
      if($categorySelect.find("option:selected").hasClass("hidden"))
        $categorySelect.val($categorySelect.find("option").not(".hidden").first().val());

    $beneficiaryAccountSelect.keydown(() -> setTimeout(() -> updateCategories()))
    $beneficiaryAccountSelect.change(updateCategories)
    updateCategories()

    ### update total ###
    $formContainer.find(".flow-as-float").keydown(() -> setTimeout(() -> updateAllTotalState($formContainer)))
    $formContainer.find(".flow-as-float").change(() -> updateAllTotalState($formContainer))

  $(".transaction-holder").each(() -> addTransactionSpecificEventListeners(this))
  $("input:radio[name=zeroSum]").change(() -> updateAllTotalState(null))
  updateAllTotalState(null)
)
