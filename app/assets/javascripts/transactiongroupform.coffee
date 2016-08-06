### utility classes ###
class DatedMoney
  constructor: (@cents, @currencyCode, @date) ->

class DatedMoneyCache
  constructor: () ->
    @cache = {}

  contains: (datedMoney) ->
    @cache[@_getCacheKey(datedMoney)] != undefined

  get: (datedMoney) ->
    @cache[@_getCacheKey(datedMoney)]

  put: (datedMoney, cents) ->
    @cache[@_getCacheKey(datedMoney)] = cents

  _getCacheKey: (datedMoney) ->
    "#{datedMoney.cents}_#{datedMoney.currencyCode}_#{datedMoney.date}"

class MoneyExchanger
  constructor: (@defaultCurrencyCode) ->
    @cache = new DatedMoneyCache()

  exchangeToDefaultAndSum: (datedMoneyArray, callback) ->
    callbacksLeft = datedMoneyArray.length
    sum = 0
    for datedMoney in datedMoneyArray
      @exchangeToDefault(datedMoney, (cents) ->
        sum += cents
        callbacksLeft--
        if(callbacksLeft == 0)
          callback(sum)
      )

  exchangeToDefault: (datedMoney, callback) ->
    @exchangeTo(datedMoney, @defaultCurrencyCode, callback)

  exchangeTo: (datedMoney, toCurrency, callback) ->
    fromCents = datedMoney.cents
    fromCurrency = datedMoney.currencyCode
    date = datedMoney.date

    if fromCurrency == toCurrency
      callback(fromCents)
    else if @cache.contains(datedMoney)
      callback(@cache.get(datedMoney))
    else
      outerThis = this
      $.post("/jsonapi/acc/money/exchange/#{fromCents}/#{fromCurrency}/#{date}/#{toCurrency}/")
        .done((result) ->
          cents = parseInt(result)
          outerThis.cache.put(datedMoney, cents)
          callback(cents)
        )

# Singleton instance
MONEY_EXCHANGER = null

### utility functions ###
parseMoneyAsFloatToCents = (moneyAsFloatString) ->
  parts = []
  moneyAsFloatString.split(",").map (e1) ->
    e1.split(".").map (e2) ->
      parts.push(e2.trim())

  dotBetweenLastElements = (list) ->
    result = ""
    i = 0
    while(i < list.length)
      result += list[i]
      if(i == list.length - 2)
        result += "."
      i++
    result
  dotBetweenLastElements(parts)

  normalized = dotBetweenLastElements(parts)
  parsed = parseFloat(normalized)
  if isNaN(parsed)
    parsed = 0
  Math.round(parsed * 100)

centsToFloatString = (inCents) ->
  afterDot = "" + Math.abs(inCents%100)
  if afterDot.length < 2
    afterDot = "0" + afterDot
  beforeDot = Math.round(Math.abs(inCents) // 100)
  sign = ""
  sign = "-" if(inCents < 0)
  "#{sign}#{beforeDot}.#{afterDot}"

getReservoirCurrencyCode = ($formContainer) ->
  $reservoirCodeSelect = $formContainer.find("select[id$=_moneyReservoirCode]")
  selectedReservoirCode = $reservoirCodeSelect.val()
  $selectedOption = $reservoirCodeSelect.find("option[value='#{selectedReservoirCode}']")
  $selectedOption.attr("currency-code")

getReservoirCurrencyIconClass = ($formContainer) ->
  $reservoirCodeSelect = $formContainer.find("select[id$=_moneyReservoirCode]")
  selectedReservoirCode = $reservoirCodeSelect.val()
  $selectedOption = $reservoirCodeSelect.find("option[value='#{selectedReservoirCode}']")
  $selectedOption.attr("currency-icon-class")

getDefaultCurrencySymbol = ($formContainer) ->
  $reservoirCodeSelect = $formContainer.find("select[id$=_moneyReservoirCode]")
  $nullReservoirOption = $reservoirCodeSelect.find("option[value='']")
  $nullReservoirOption.attr("currency-code")

getTransactionDate = ($formContainer) ->
  $formContainer.find("input[id$=_transactionDate]").val()

getBeneficiaryAccountCategoryCodes = ($formContainer) ->
  $beneficiaryAccountSelect = $formContainer.find("select[id$=_beneficiaryAccountCode]")
  accountCode = $beneficiaryAccountSelect.val()
  $selectedOption = $beneficiaryAccountSelect.find("option[value='#{accountCode}']")
  $selectedOption.attr("category-codes").split(' ')

### update total functions ###
updateAllTotalState = ($thisFormContainer) ->
  getTotalInCentsFromInputs = (callback) ->
    datedMoneyArray = []
    $(".transaction-holder").each () ->
      $formContainer = $(this)
      cents = parseMoneyAsFloatToCents($formContainer.find(".flow-as-float").val())
      currencyCode = getReservoirCurrencyCode($formContainer)
      date = getTransactionDate($formContainer)
      datedMoneyArray.push(new DatedMoney(cents, currencyCode, date))

    MONEY_EXCHANGER.exchangeToDefaultAndSum(datedMoneyArray, callback)

  isZeroSumForm = () ->
    $("input:radio[name=zeroSum]:checked").val() == "true"

  fixTotalZeroIfNotLast = (totalInCents, callback) ->
    $lastContainer = $(".transaction-holder").last()
    isLast = $lastContainer.is($thisFormContainer)
    numForms = $(".transaction-holder").length
    if isZeroSumForm() and not isLast and numForms > 1
      lastValue = parseMoneyAsFloatToCents(($lastContainer.find(".flow-as-float").val()))
      lastCurrencyCode = getReservoirCurrencyCode($lastContainer)
      lastConsumedDate = getTransactionDate($lastContainer)
      datedMoney = new DatedMoney(totalInCents, MONEY_EXCHANGER.defaultCurrencyCode, lastConsumedDate)
      MONEY_EXCHANGER.exchangeTo(datedMoney, lastCurrencyCode, (totalInCents) ->
        newLastValue = lastValue - totalInCents
        $lastContainer.find(".flow-as-float").val(centsToFloatString(newLastValue))
        callback(0)
      )
    else
      callback(totalInCents)

  updateTotal = (totalInCents) ->
    if totalInCents == null
      $(".total-transaction-flow").html("...")
    else
      $(".total-transaction-flow").html(centsToFloatString(totalInCents))

  updateTotalColor = (totalInCents) ->
    zeroSum = isZeroSumForm()
    $(".total-flow-text").toggleClass("nonzero-warning", zeroSum && totalInCents != 0)

  updateTotal(null)
  updateTotalColor(0)
  getTotalInCentsFromInputs (totalInCents) ->
    fixTotalZeroIfNotLast(totalInCents, (totalInCents) ->
      updateTotal(totalInCents)
      updateTotalColor(totalInCents)
    )

### setup descriptions' typeahead ###
setupDescriptionsTypeahead = (formContainer) ->
  $formContainer = $(formContainer)

  $formContainer.find('.description').typeahead(
    {
      highlight: true,
      minLength: 1,
    },
    {
      name: 'description',
      limit: 30, # Explicitly setting limit because the default limit (5) surfaces a bug
                 # where typeahead only shows (limit - numResults) results.
      source: new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.whitespace,
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        remote: {
          url: '/jsonapi/acc/descriptions/null/null/null/%QUERY',
          prepare: ((query) ->
            beneficiaryCode = $formContainer.find('.beneficiaryAccountCode').val()
            reservoirCode = $formContainer.find('.moneyReservoirCode').val()
            categoryCode = $formContainer.find('.categoryCode').val()
            query = query
                .replaceAll("%", "%25") # has to come first
                .replaceAll("#", "%23")
                .replaceAll("&", "%26") # TODO replace all
                .replaceAll("+", "%2B")
                .replaceAll(";", "%3B")
            {
              url: "/jsonapi/acc/descriptions/#{beneficiaryCode}/#{reservoirCode}/#{categoryCode}/?q=#{query}",
            }
          )
        }
      })
    }
  )

### setup bootstrap-tagsinput ###
setupBootstrapTagsinput = (formContainer) ->
  $formContainer = $(formContainer)

  bloodhound = new Bloodhound({
    datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
    queryTokenizer: Bloodhound.tokenizers.whitespace,
    prefetch: {
      url: '/jsonapi/acc/tags/all/',
      cache: false,
      filter: ((list) ->
        $.map(list, (tagname) -> {name: tagname})),
    }
  })
  bloodhound.initialize()

  $formContainer.find('input.tags-input').tagsinput({
    confirmKeys: [13, 32, 44, 46], # 13=newline, 32=space, 44=comma, 46=dot
    tagClass: (item) ->
      sha1Val = sha1(item)

      stringToHashedInt = (s) ->
        hash = 0
        len = s.length
        if(len == 0)
          hash
        else
          i = 0
          while(i < len)
            chr   = s.charCodeAt(i)
            hash  = ((hash << 5) - hash) + chr
            hash |= 0 # Convert to 32bit integer
            i++
          hash
      hashedInt = stringToHashedInt(sha1Val)

      bootstrapClassSuffixOptions = ["primary", "success", "info", "warning", "danger"]
      index = Math.abs(hashedInt) % bootstrapClassSuffixOptions.length
      bootstrapClassSuffix = bootstrapClassSuffixOptions[index]
      "label label-#{bootstrapClassSuffix}"
    ,
    typeaheadjs: {
      name: 'tagnames',
      displayKey: 'name',
      valueKey: 'name',
      source: bloodhound.ttAdapter()
    },
  })

  # Bugfix for tagsinput + typeahead.js: When losing focus, the last tag was not created
  $formContainer.find('.bootstrap-tagsinput').focusout(() ->
    $tagsinput_generated = $(this)
    $tagsinput_generated_input = $tagsinput_generated.find("input.tt-input")
    $tagsinput_coded = $tagsinput_generated.parent().find(".tags-input")

    leftover_value = $tagsinput_generated_input.val()
    if(leftover_value)
      $tagsinput_coded.tagsinput('add', leftover_value)
  )

### setup flow currency update ###
setupFlowCurrencyUpdate = (formContainer) ->
  $formContainer = $(formContainer)

  updateFlowCurrency = () ->
    currencyIconClass = getReservoirCurrencyIconClass($formContainer)
    $formContainer.find(".currency-indicator").html("<i class='#{currencyIconClass}'></i>")

    updateAllTotalState($formContainer)

  $reservoirCodeSelect = $formContainer.find("select[id$=_moneyReservoirCode]")
  $reservoirCodeSelect.keydown(() -> setTimeout(() -> updateFlowCurrency()))
  $reservoirCodeSelect.change(updateFlowCurrency)
  updateFlowCurrency()

$(document).ready(() ->
  ### constants ###
  ROOT_FORM_CONTAINER = $('#transaction-holder-0')
  MONEY_EXCHANGER = new MoneyExchanger(getDefaultCurrencySymbol(ROOT_FORM_CONTAINER))

  ### make add-transaction window have the same height as all other windows ###
  $('.add-transaction-button-holder .panel-body').height($('#transaction-holder-0 .panel-body').height())

  ### define TransactionNumGenerator ###
  class TransactionNumGenerator
    highestTransactionNum: 0
    constructor: () ->
      while($("#transaction-holder-#{@highestTransactionNum+1}").length)
        @highestTransactionNum++
    getNext: () ->
      ++@highestTransactionNum
  transactionNumGenerator = new TransactionNumGenerator()

  ### new transaction behaviour ###
  $(".add-transaction-button").click(() ->
    transactionNum = transactionNumGenerator.getNext()
    newForm = ROOT_FORM_CONTAINER.clone()

    # bugfix in clone(): manually copying select selection and textarea content
    newForm.find('select').each((index, item) -> $(item).val(ROOT_FORM_CONTAINER.find('select').eq(index).val()))
    newForm.find('textarea').each((index, item) -> $(item).val(ROOT_FORM_CONTAINER.find('textarea').eq(index).val()))

    # bugfix in clone() + bootstrap-tagsinput: restore regular input and re-run setup
    newForm.find('.bootstrap-tagsinput').each((index, item) ->
      $(item).remove()
    )
    setupBootstrapTagsinput(newForm)
    # Twitter typeahead fix: Restore regular input and re-run setup
    newForm.find('span.twitter-typeahead').each (index, item) ->
      $replacement = $(item).find(".tt-input")
      $(item).replaceWith($replacement)

    setupDescriptionsTypeahead(newForm)
    setupFlowCurrencyUpdate(newForm)

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
      updateAllTotalState(null)
    )

    ### enforce bind-until-change ###
    $formContainer.find(".bind-until-change").each(() ->
      # get boundSources
      getBoundSources = (boundElem) ->
        elemName = boundElem.attr('name')
        # Twitter typeahead fix: Typeahead creates two inputs (only one with a name)
        if(!elemName)
          return []
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
        sources

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

          # Twitter typeahead fix: Typeahead creates an enclosing span.twitter-typeahead
          # that should be the one changing color.
          boundElem.parent(".twitter-typeahead").toggleClass("bound-until-change", equalToAnySource)

        updateBoundedState()
        boundElem.keyup(() -> setTimeout(updateBoundedState))
        boundElem.change(() -> setTimeout(updateBoundedState))
        # Twitter typeahead fix: It's tricky to get the old value and this isn't really
        # necessary when there is no transitive dependency
        if(boundElem.hasClass("tt-input"))
          boundSources.keydown (e) ->
            sourceElem = $(this)
            setTimeout () ->
              newSourceElemValue = sourceElem.val()
              if(boundElem.hasClass("bound-until-change"))
                boundElem.val(newSourceElemValue)
              updateBoundedState()

        else if(boundElem.attr("type") == "text" || boundElem[0].tagName == "TEXTAREA")
          boundSources.keydown((e) ->
            sourceElem = $(this)
            oldSourceElemValue = sourceElem.val()
            boundElemValue = boundElem.val()
            setTimeout(() ->
              newSourceElemValue = sourceElem.val()
              if(boundElem.hasClass("bound-until-change") && oldSourceElemValue == boundElemValue)
                boundElem.val(newSourceElemValue)

              updateBoundedState()
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
          boundSources.keydown () ->
            $sourceElem = $(this)
            setTimeout () -> $sourceElem.change()
    )

    ### enforce bind-tags-input-until-change-to-root ###
    $formContainer.find(".bind-tags-input-until-change-to-root").each(() ->
      # get boundSource
      boundInputElem = $(this)
      elemName = boundInputElem.attr('name')
      isRootElem = elemName.contains("[0]")
      if(isRootElem)
        return # early
      sourceElemName = elemName.replace(/\[\d\]/, "[0]")
      boundSource = ROOT_FORM_CONTAINER.find("[name='#{sourceElemName}']")

      # get boundTagsinputElem
      boundTagsinputElem = () -> boundInputElem.parent().find(".bootstrap-tagsinput")

      # update bounded-state
      updateBoundedState = () ->
        equalToSource = false
        if(boundSource.val() == boundInputElem.val())
          equalToSource = true
        boundTagsinputElem().toggleClass("bound-until-change", equalToSource)
        equalToSource

      setTimeout(() -> # allow boundTagsinputElem to be rendered first
        updateBoundedState()
      )
      boundInputElem.on('itemAdded', updateBoundedState)
      boundInputElem.on('itemRemoved', updateBoundedState)
      boundSource.on('itemAdded', (event) ->
        if(boundTagsinputElem().hasClass("bound-until-change"))
          boundInputElem.tagsinput('add', event.item)
        else
          updateBoundedState() # maybe now the source is again equal to the bounded elem
      )
      boundSource.on('itemRemoved', (event) ->
        if(boundTagsinputElem().hasClass("bound-until-change"))
          boundInputElem.tagsinput('remove', event.item)
        else
          updateBoundedState() # maybe now the source is again equal to the bounded elem
      )
    )

    ### filter categories, based on current beneficiaryAccount ###
    $beneficiaryAccountSelect = $formContainer.find("select[id$=_beneficiaryAccountCode]")
    $categorySelect = $formContainer.find("select[id$=_categoryCode]")
    updateCategories = () ->
      categoryCodes = getBeneficiaryAccountCategoryCodes($formContainer) # in order
      currentCategory = $categorySelect.val()
      # update option's hidden state
      $categorySelect.find("option").each(() ->
        $option = $(this)
        $option.toggleClass("hidden", $option.val() not in categoryCodes)
      )
      # adapt ordering to the one of categoryCodes
      $firstUnorderedOption = $categorySelect.find("option").first()
      first = true
      for nextCategoryCode in categoryCodes
        $nextOption = $categorySelect.find("option[value='#{nextCategoryCode}']")
        if first
          first = false
          $firstUnorderedOption.before($nextOption)
        else
          $previousOption.after($nextOption)
        $previousOption = $nextOption
      # make sure the current value is not hidden
      if($categorySelect.find("option:selected").hasClass("hidden"))
        $categorySelect.val($categorySelect.find("option").not(".hidden").first().val())

    $beneficiaryAccountSelect.keydown(() -> setTimeout(() -> updateCategories()))
    $beneficiaryAccountSelect.change(updateCategories)
    updateCategories()

    ### update total ###
    $formContainer.find(".flow-as-float").keydown(() -> setTimeout(() -> updateAllTotalState($formContainer)))
    $formContainer.find(".flow-as-float").change(() -> updateAllTotalState($formContainer))
    $formContainer.find("input[id$=_transactionDate]").keydown(() -> setTimeout(() -> updateAllTotalState($formContainer)))
    $formContainer.find("input[id$=_transactionDate]").change(() -> updateAllTotalState($formContainer))

  $(".transaction-holder").each(() -> addTransactionSpecificEventListeners(this))
  $(".transaction-holder").each(() -> setupDescriptionsTypeahead(this))
  $(".transaction-holder").each(() -> setupBootstrapTagsinput(this))
  $(".transaction-holder").each(() -> setupFlowCurrencyUpdate(this))
  $("input:radio[name=zeroSum]").change(() -> updateAllTotalState(null))
  updateAllTotalState(null)
)
