Family Accounting Tool
======================

[![CI Status](https://github.com/nymanjens/facto/actions/workflows/ci.yml/badge.svg)](https://github.com/nymanjens/facto/actions)

Family Accounting Tool keeps track of every transaction your family makes. Its goal is to:

- Extract useful statistics from the data<br>
  _E.g.: How much is spent on food each month?_
- Calculate internal debt<br>
  _E.g.: Alice buys toilet paper for the whole family with her own money. This tool then registers
  that the family owes Alice the cost of the toilet paper._
- Provide a way to check if no money is disappearing in unexpected expenses<br>
  _E.g.: You will notice when there are hidden banking costs_

## Screenshot

![screenshot](screenshot.png "Screenshot")

## Installation from release

- Install Java 11 (JDK 11) on your server
- Download "Binaries (compiled files) with demo configuration" from the [latest
  release](https://github.com/nymanjens/facto/releases)
- Unpack the archive
- Open `conf/application.conf` in the unpacked folder:
  - Configure a database. The easiest way is to set up a MariaDB server locally,
    create an empty database called `facto` and configure it as follows:

```
db.default {
  driver = com.mysql.jdbc.Driver
  url = "jdbc:mysql://localhost/facto?user=mysqluser&password=mysqlpassword"
  slick.profile = "slick.jdbc.MySQLProfile$"
}
```

  - Choose secret values for `play.http.secret.key` and `app.setup.defaultPassword`

- Open a terminal in the unpacked folder
- Run following commands to get the app running:

    ```
    # Create database tables
    bin/server -DdropAndCreateNewDb

    # Create admin user
    bin/server -DcreateAdminUser

    # Run application
    bin/server
    ```

- Browse to http://localhost:9000/app/useradministration (username: "admin", password: "changeme")

## Installation with Docker

Move to the directory where the `docker-compose.yml` file and `.env` file will be stored, e.g. `~/facto`:

```
mkdir ./facto
cd ./facto
```

Download the `docker-compose.yml` and `.env` file:
```
wget -O docker-compose.yml https://github.com/nymanjens/facto/master/docker/docker-compose.yml
wget -O .env https://github.com/nymanjens/facto/master/docker/example.env
```

You can also download the files directly from the repo and place them in the directory you created above.
Then, edit the `.env` file to set your database password, the app secret and the default setup password to a random string of sufficient length.:
```
cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1
```

Finally, you can start the application with Docker Compose:
```
docker-compose up
```

When done, browse to http://localhost:9000/ (username: "admin", password: "changeme")

## Configuration
- `conf/application.conf`:<br>
  Setup and configure a database here.
- Add users:<br>
  A default user is created with the `-DcreateAdminUser` flag (username: admin, password:
  changeme). This account can create new users.
- `conf/accounting-config.yml`:<br>
  Edit this file to reflect your situation and preferences. Note that
  `userLoginName` has to refer to a user that already exists in the database
  (see previous point)

## Usage
To effectively use this tool, you must learn to input the transactions correctly. Therefore,
it's important to understand the meaning of categories and accounts.

### Categories
These are used to split up your expenses into categories. Every account has some
special categories:

- **Accounting category:**<br>
  Used for non-real income/expenses. This category will be ignored when analysing the monthly
  surplus/deficit in terms of expenses. Use this category e.g. when the expense will be paid
  back by your employer
- **Endowment category:**<br>
  Used for money transfer to the common account (see example in the next section)

### Accounts
An account can refer to a regular person or the common account.

**Example:**

A family has two members: Alice and Bob. There will be three accounts:

- Alice
- Bob
- The common account

Alice and Bob will regularly transfer money to the common account which can be used to make
common expenses. This transfer to the common account is called an _'endowment'_.

### Inputting transactions
Special fields:

- **Beneficiary:**<br>
  In case of an expense: The one that will benefit from the expense<br>
  In case of an income: The one that was paid
- **Paid with/to:**<br>
  In case of an expense: Paid with<br>
  In case of an income: Paid to
- **Flow:**<br>
  In case of an expense: Negative number representing the price<br>
  In case of an income: Positive number

### Using the Cash Flow overview effectively
The cash flow overview is a powerfull tool to check for unaccounted expenses.

You should:

- Set the current amount of money you have for every payment method (cash, card, ...).
  You can do this by using the _"set"_ button.
- Whenever you perform a transaction, the balance will be updated automatically.
- You can re-count your cash and see if it matches the calculated value.
  If it does, you can use the checkbox button to mark this.
  If it doesn't, you have made an error or lost money.

## About this project

This project was created and maintained by me (Jens) in my spare time for my
family, and I made it open source in the hope that it might be useful for
others.

If you decide to use this, chances are you'll either be using it a lot, or stop
using it after some time. If you are in the former category, please send me a
message at nymanjens.nj@gmail.com (or consider [sending a
donation](https://github.com/sponsors/nymanjens)) so I know others are using
this. Definitely also let me know if you have any interest in upgrading to newer
versions or using the more advanced (hidden) features like exchange rates and
inflation correction.
