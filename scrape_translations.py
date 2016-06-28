# -*- coding: utf-8 -*-

import os
import re

def generate_code(words_html):
  words = re.sub("<.*?>", "", words_html) # remove tags
  code = words.strip().replace(" ", "-").lower()
  code = code.replace("/", "-")
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

def strip_prefix(prefix, s):
  assert s.startswith(prefix)
  return s[len(prefix):]

def main():
  for root, subdirs, files in os.walk('app'):
    for filename in files:
      if filename.endswith(".scala.html") or  filename.endswith(".scala"):
        path = os.path.join(root, filename)
        fcontent = open(path).read()
        new_fcontent = fcontent
        # Example: '~Successfully deleted balance check£'
        for words in re.findall(r'~(.*?)£', fcontent):
          match = "~{}£".format(words)
          code = generate_code(words)
          new_fcontent = new_fcontent.replace(match, '@Messages("{}")'.format(code))
          add_to_all_messages(code, words)
          print path, generate_code(words)
        # Example: '"∞Successfully deleted balance check£"'
        for words in re.findall(r'"¢(.*?)£"', fcontent):
          match = '"¢{}£"'.format(words)
          code = generate_code(words)
          new_fcontent = new_fcontent.replace(match, 'Messages("{}")'.format(code))
          add_to_all_messages(code, words)
          print path, generate_code(words)
        # Example: '"∞Successfully deleted balance check for§ $moneyReservoirName£"'
        for string in re.findall(r'"∞(.*?§.*?)£"', fcontent):
          match = '"∞{}£"'.format(string)
          parts = string.split('§')
          assert len(parts) == 2
          words = parts[0]
          code = generate_code(words)
          variables = [strip_prefix("$", v) for v in re.findall(r'\$\w+|\${.*?}', parts[1])]
          new_fcontent = new_fcontent.replace(match, 'Messages("{}", {})'.format(code, ', '.join(variables)))

          templated_words = string
          old_templated_words = None
          ctr = 0
          while old_templated_words != templated_words:
            old_templated_words = templated_words
            templated_words = re.sub(r'\$\w+|\${.*?}', '{%s}' % ctr, templated_words, count=1)
            ctr += 1

          add_to_all_messages(code, templated_words)

          print path, generate_code(words)
        open(path, "w").write(new_fcontent)

if __name__ == '__main__':
  main()
