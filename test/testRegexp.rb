require 'test/minirunit'
test_check "Test regexp substitutions:"

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

#########    test6   #################
str4 =  'B'
test_ok(0 == (str4 =~ /^(?:(A):)?(B)/))
test_ok(nil == $1)
test_ok(str4 == $2)

#########    test7   #################
test_ok("(?-mix:pattern)" == /pattern/.to_s)
test_ok("(?m-ix:pattern)" == /pattern/m.to_s)
test_ok("(?mix:pattern)" == /pattern/mix.to_s)

#########    test8   #################
test_ok(/ab (?# comment )c/ =~ 'ab c')

#########    test9   #################
test_ok("\tZ"   =~ /\x9Z/)
test_ok("\t"   =~ /\x9/)
test_ok("\tZ\tQ"   =~ /\x9Z\x9Q/)
test_ok("\x9cZ" =~ /\x9cZ/)

#########    test10   #################
'ABCDE' =~ /B(C)D/
test_equal('BCD', $~.to_s)

"ALBUM: Foo Bar".match(/ALBUM: [^\s]*\s(.+)/)
test_equal('Bar', $1)

######## MatchData #############
match_data = /(.)(.)(\d+)(\d)/.match("THX1138")

test_equal(["HX1138", "H", "X", "113", "8"], match_data.to_a)
test_equal(["H", "X", "113", "8"], match_data.captures)

##### === ######
/a/ === :a
