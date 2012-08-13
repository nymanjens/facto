#!/usr/bin/env python
# author: Jens Nyman (nymanjens.nj@gmail.com)

import urllib2
from os.path import dirname

# settings
SECRET_KEY = "<cfr. settings_local.php>"
SITE_URL = "https://example.com"
BACKUP_FILE = dirname(__file__) + '/../backup.sql'


# get backup data
url = SITE_URL + '/admin/export?SECRET_KEY=%s' % (SECRET_KEY)
#print url
sql_data = urllib2.urlopen(url).read()

# write data to backup file
f = open(BACKUP_FILE, 'w')
f.write(sql_data)

