# -*- coding: utf-8 -*-

import os
import re

def generate_code(words_html):
  words = re.sub("<.*?>", "", words_html) # remove tags
  code = words.strip().replace(" ", "-").lower()
  assert re.match(r'^[a-z0-9-]+$', code), "Illegal code: {}".format(code)
  return "facto.{}".format(code)

def add_to_messages(fpath, code, words):
  if code not in open(fpath).read():
    line_to_add = "{}={}\n".format(code, words)
    open(fpath, 'a').write(line_to_add)

def add_to_all_messages(code, words):
  add_to_messages("conf/messages", code, words)
  add_to_messages("conf/messages.nl", code, "XX" + words)

def main():
  for root, subdirs, files in os.walk('.'):
    for filename in files:
      if filename.endswith(".scala.html") or  filename.endswith(".scala"):
        path = os.path.join(root, filename)
        fcontent = open(path).read()
        new_fcontent = fcontent
        for words in re.findall(r'~(.*?)£', fcontent):
          match = "~{}£".format(words)
          code = generate_code(words)
          new_fcontent = new_fcontent.replace(match, '@Messages("{}")'.format(code))
          add_to_all_messages(code, words)
          print path, generate_code(words)
        open(path, "w").write(new_fcontent)

if __name__ == '__main__':
  main()
