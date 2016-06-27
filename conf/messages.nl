#
# Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
#

# Default messages

# --- Constraints
constraint.required=Required
constraint.min=Minimum value: {0}
constraint.max=Maximum value: {0}
constraint.minLength=Minimum length: {0}
constraint.maxLength=Maximum length: {0}
constraint.email=Email

# --- Formats
format.date=Date (''{0}'')
format.numeric=Numeric
format.real=Real
format.uuid=UUID

# --- Errors
error.invalid=Invalid value
error.invalid.java.util.Date=Invalid date value
error.required=This field is required
error.number=Numeric value expected
error.real=Real number value expected
error.real.precision=Real number value with no more than {0} digit(s) including {1} decimal(s) expected
error.min=Must be greater or equal to {0}
error.min.strict=Must be strictly greater than {0}
error.max=Must be less or equal to {0}
error.max.strict=Must be strictly less than {0}
error.minLength=Minimum length is {0}
error.maxLength=Maximum length is {0}
error.email=Valid email required
error.pattern=Must satisfy {0}
error.date=Valid date required
error.uuid=Valid UUID required

error.expected.date=Date value expected
error.expected.date.isoformat=Iso date value expected
error.expected.time=Time value expected
error.expected.jodadate.format=Joda date value expected
error.expected.jodatime.format=Joda time value expected
error.expected.jsarray=Array value expected
error.expected.jsboolean=Boolean value expected
error.expected.jsnumber=Number value expected
error.expected.jsobject=Object value expected
error.expected.jsstring=String value expected
error.expected.jsnumberorjsstring=String or number expected
error.expected.keypathnode=Node value expected
error.expected.uuid=UUID value expected
error.expected.validenumvalue=Valid enumeration value expected
error.expected.enumstring=String value expected

error.path.empty=Empty path
error.path.missing=Missing path
error.path.result.multiple=Multiple results for the given path

# --- Project-specific messages
facto.error.noReservoir.atLeast2=To be able to use the N/A money reservoir, there must be at least two transactions
facto.error.noReservoir.zeroSum=To be able to use the N/A money reservoir, the total flow of this group must sum to 0
facto.error.noReservoir.notAllTheSame=The N/A money reservoir can be selected for either all transactions or no transactions
facto.user-profile=XXUser Profile
facto.user-administration=XXUser Administration
facto.update-logs=XXUpdate Logs
facto.logout=XXLogout
facto.everything=XX<u>E</u>verything
facto.cash-flow=XX<u>C</u>ash Flow
facto.liquidation=XX<u>L</u>iquidation
facto.endowments=XXEn<u>d</u>owments
facto.summary=XX<u>S</u>ummary
facto.new-entry=XX<u>N</u>ew Entry
facto.templates=XX<u>T</u>emplates
