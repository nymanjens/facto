# Family Accounting Tool

The Familiy Accounting Tool keeps track of every transaction your family makes. Its goal is to:
  * Extract usefull statistics from the data _(e.g. how much do you spend on food per month?)_
  * Calculate internal debt. _For example: Alice buys toilet paper for the whole family with her own money. This tool then registers that the family ows Alice the cost of the toilet paper._
  * Provide a way to check if no money is disappearing in unexpected expenses (like banking costs or lost cash)


## Demo
Try it out yourself using [the online demo](http://www.totw.nl/demo/).
  * Login: alice
  * Password: alice


## Requirements
Webserver (local or on the internet) with:
  * php 5
  * MySQL
  * Apache


## Installation
  * enable mod_rewrite in Apache
  * make /library/dwoo/compiled writable for apache
  * load structure.sql into a database table


## Configuration
#### Configuration files
Please modify the files:
  * settings.php
  * settings_accounting.php
  * settings_local.php

as many default settings are dummy settings.

#### Creating user accounts
An admin account is created by default with login:
  * login name: admin
  * password:   admin

make sure to change this password.

Now you can create an account for each participating family member, using the 'add user'-tool
in the Admin tab. Please make sure that the name corresponds with the account name you chose
in settings_accounting.php.


## Usage
The hardest part of using this tool is to correctly input the transactions. Therefore, it's important
to understand the meaning of the categories and accounts.

#### Categories
These are used to split up your expenses into categories. Every account has some special
special categories:
  * accounting category: used for non-real income/expenses. This category will be ignored when analysing your montly surplus/deficit in terms of expenses. Use this category e.g. when your expense will be payed back by your employer
  * endowment category: used for money transfer to the common account (see example in the next section)

#### Accounts
An account can refer to a regular person or the common account.

_For example: a family has two members: Alice and Bob. <br>
There will be three accounts: Alice, Bob and the common account. Alice and Bob will regularly
transfer money to the common account which can be used to make common expenses. This transfer
to the common account is called an 'endowment'._

#### Inputting transactions
_Regular entry_<br>
Use this for everything that is not covered in the other special entries.
special fields:
  * beneficiary:<br>in case of an expense: the one that will benefit from the expense<br>in case of an income: the one that was paid
  * payed with/to:<br>in case of an expense: payed with<br>in case of an income: payed to
  * Flow:<br>in case of an expense: negative number representing the price<br>in case of an income: positive number

_Withdrawal_ <br>
Use this when you withdrew money from your bank account.

_Endowment_ <br>
Use this when you transfer money from your account to the common account.

#### Using the Cash Flow overview
The cash flow overview is a very powerfull tool to check for unaccounted expenses.

Firstly, you should set the current amount of money you have for every payment method (cash,
card, ...). You can do this by using the "set balance" link. Thereafter, whenever you perform
a transaction, the balance will be updated. Later, you can re-count your cash and see if it
matches the calculated value. If it does, you can use the "set balance" link and press "Ok"
right away. It will annote the balance as verified. If the money you counted does not match
the calculated value, you have made an error or lost money.
