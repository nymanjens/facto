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
        # Example: 's"∞Successfully deleted balance check for§ $moneyReservoirName£"'
        for string in re.findall(r's"∞(.*?§.*?)£"', fcontent):
          match = 's"∞{}£"'.format(string)

          code = None
          text = None
          if '¶' in string:
            p = string.split('¶')
            assert len(p) == 2
            code = p[0]
            text = p[1]
          else:
            parts = string.split('§')
            assert len(parts) == 2
            words = parts[0]
            if not code:
              code = generate_code(words)
            text = ''.join(parts)

          variables = [strip_prefix("$", v) for v in re.findall(r'\$\w+|\${.*?}', text)]
          new_fcontent = new_fcontent.replace(match, 'Messages("{}", {})'.format(code, ', '.join(variables)))

          templated_text = text
          old_templated_text = None
          ctr = 0
          while old_templated_text != templated_text:
            old_templated_text = templated_text
            templated_text = re.sub(r'\$\w+|\${.*?}', '{%s}' % ctr, templated_text, count=1)
            ctr += 1

          add_to_all_messages(code, templated_text)

          print path, generate_code(words)
        open(path, "w").write(new_fcontent)

if __name__ == '__main__':
  main()
