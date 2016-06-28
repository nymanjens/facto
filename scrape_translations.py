# -*- coding: utf-8 -*-

import os
import re

def generate_code(words_html):
  words = re.sub("<.*?>", "", words_html) # remove tags
  words = words.replace("/", "-")
  code = words.strip().replace(" ", "-").lower()
  assert re.match(r'^[a-z0-9-]+$', code), "Illegal code: {}".format(code)
  assert "--" not in code, "Illegal code: {}".format(code)
  if words_html != words:
    return "facto.{}.html".format(code)
  else:
    return "facto.{}".format(code)

def add_to_messages(fpath, code, words):
  line_to_add = "{}={}\n".format(code, words)
  if code+"=" in open(fpath).read():
    lines = open(fpath).readlines()
    matching_lines = [l for l in lines if l.startswith(code+"=")]
    assert len(matching_lines) == 1
    assert matching_lines[0] == line_to_add, "Conflict for code {}:\nExpected:\n{}Got:\n{}".format(code, line_to_add, matching_lines[0])
  else:
    open(fpath, 'a').write(line_to_add)

def add_to_all_messages(code, words):
  add_to_messages("conf/messages", code, words)
  add_to_messages("conf/messages.nl", code, "XX" + words)

def main():
  for root, subdirs, files in os.walk('app'):
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
        for words in re.findall(r'"¢(.*?)£"', fcontent):
          match = '"¢{}£"'.format(words)
          code = generate_code(words)
          new_fcontent = new_fcontent.replace(match, 'Messages("{}")'.format(code))
          add_to_all_messages(code, words)
          print path, generate_code(words)
        open(path, "w").write(new_fcontent)

if __name__ == '__main__':
  main()
