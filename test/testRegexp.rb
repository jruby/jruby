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
test_equal(false, /a/ === :a)
test_equal(false, /aa/ === ['a' => 'a'])
test_equal(false, :a =~ /a/)
test_equal(false, ['a' => 'a'] =~ /a/)

##### inspect #####
re = /^admin\/.+$/

test_equal("^admin\\/.+$", re.source)
test_equal("/^admin\\/.+$/", re.inspect)

re = Regexp.new("/hi/")
test_equal("/hi/", re.source)
test_equal("/\\/hi\\//", re.inspect)

##### Posix sequences ######
"a  b" =~ /([[:space:]]+)/
test_equal("  ", $1)
# We should only honor this as posix sequence inside [] (bug 1475096)
#test_equal(0, "a  b" =~ /([:space:]+)/)

##### union #####
test_equal(/(?!)/, Regexp.union)
test_equal(/penzance/, Regexp.union("penzance"))
test_equal(/skiing|sledding/, Regexp.union("skiing", "sledding"))
test_equal(/(?-mix:dogs)|(?i-mx:cats)/, Regexp.union(/dogs/, /cats/i))
