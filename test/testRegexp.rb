#########    test1   #################
rgx1 = /[a-z]+/
str1 = "redrum".sub(rgx1, "<\\&>")
test_ok(str1=='<redrum>')

#########    test2   #################
str1.sub!(/\w+/) { |m| $` + m.upcase + $' }

#########    test3   #################
test_ok(str1=='<<REDRUM>>')

#########    test4   #################
test_ok('*R*U*B*Y*' ==  "ruby".upcase.gsub(/\d?/, '*'))

#########    test5   #################
str3 = "regnad kcin".reverse
str3.gsub!(/\w+/) { |m| m.capitalize }
test_ok('Nick Danger' == str3)

