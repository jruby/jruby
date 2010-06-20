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

# copied from MRI sample/test.rb
a = []
(0..255).each {|n|
  ch = [n].pack("C")                     
  a.push ch if /a#{Regexp.quote ch}b/x =~ "ab" 
}
test_ok(a.size == 0)

# In Ruby 1.8.5, quote can take an optional encoding arg
test_equal("hel\\\\l\\*o\317\200", Regexp.quote("hel\\l*o\317\200", "n"))
test_equal("hel\\\\l\\*o\317\200", Regexp.quote("hel\\l*o\317\200", "u"))

# test matching \r
test_equal("\r", /./.match("\r")[0])

#test_exception(SyntaxError) { r = /*/ }

test_equal(/a/.hash, /a/.hash)

test_equal("\\-", Regexp.quote('-'))

# JRUBY-722
/a((abc)|(foo))d/ =~ "afood"
test_equal ["foo", nil, "foo"], $~.captures

# JRUBY-741
test_equal "foo", Regexp.last_match(1)

# JRUBY-717
test_equal nil, /./ =~ "\n"
test_equal 0, /(?m:.)/ =~ "\n"


NAME_STR= '[\w]*'
TAG_MATCH = /^<#{NAME_STR}\s*>/u
input = <<-EOL
<path
  >
EOL
test_ok TAG_MATCH =~ input

xy = /(.*).*\1\}/
xy =~ "12}"

/1{2}+/ =~ "1111"
test_equal $&, "1111"
/1{2}+/ =~ "111111111111"
test_equal $&, "111111111111"
/1{2}+/ =~ "111"
test_equal $&, "11"

# JRUBY-139: don't show result of internal JRuby translations
test_equal("/[:alpha:]/", %r{[:alpha:]}.inspect)
test_equal("[:alpha:]", %r{[:alpha:]}.source)

# Why anyone would do this I have no idea, but it matches MRI
test_equal(/x/, +/x/)


def m(it = false)
  a = /a/
  a.instance_variable_set :@set, true if it
  a
end

test_equal nil, m().instance_variable_get(:@set)
test_equal true, m(true).instance_variable_get(:@set)
test_equal true, m().instance_variable_get(:@set)

# JRUBY-1046: Support \G correctly:
test_equal ["aa1 ", "aa2 "], "aa1 aa2 ba3 ".scan(/\Ga+\d\s*/)

# JRUBY-1109: Octal literals eat next character...
test_equal 0, "\034\015" =~ /^\034\015$/

test_equal 0, /\0/ =~ "\0"
test_equal 0, /\00/ =~ "\0"
test_equal 0, /\00/ =~ "\0"

# JRUBY-1372: Regexp.quoting
old_kcode = $KCODE
$KCODE = 'u'
helpers_dir = "/my/happy/helpers"
extract = /^#{Regexp.quote(helpers_dir)}\/?(.*)_helper.rb$/
test_equal "/^\\/my\\/happy\\/helpers\\/?(.*)_helper.rb$/", extract.inspect
$KCODE = old_kcode

# JRUBY-1385: String.match shouldn't return the same object
pattern = /\w/
ext = ["foo", "bar"].map {|e| e.match(pattern) }
test_ok ext[0] != ext[1]

# MatchData#select behavior
# FIXME: the select behavior below matches MRI rdoc, but not MRI behavior
m = /(.)(.)(\d+)(\d)/.match("THX1138: The Movie")
test_equal(["HX1138", "H", "X", "113", "8"], m.to_a)
#test_equal(["HX1138", "H", "X", "113", "8"], m.select {})
#test_equal(["HX1138", "X", "113"], m.select(0, 2, -2) {})

# JRUBY-1236
test_equal(0, "\n" =~ /\s/n)

# JRUBY-1552
test_equal(1, Array('a'..'z').map { |c| c.to_s[/#{c}/o] }.compact.size)
test_equal(26, Array('a'..'z').map { |c| c.to_s[/#{c}/] }.compact.size)

test_equal nil, /[^a]/i =~ "a"
test_equal nil, /[^a]/i =~ "A"
test_equal nil, /[^A]/i =~ "a"
test_equal nil, /[^A]/i =~ "A"

test_equal nil, /[^a-c]/i =~ "b"
test_equal nil, /[^a-c]/i =~ "B"
test_equal nil, /[^A-C]/i =~ "b"
test_equal nil, /[^A-C]/i =~ "B"

test_ok Regexp.new("foo").send(:initialize, "bar")
test_ok Regexp.new("foo").send(:initialize_copy, Regexp.new("bar"))
test_exception(SecurityError){/foo/.send(:initialize, "bar")}
test_exception(SecurityError){/foo/.send(:initialize_copy, Regexp.new("bar"))}
test_no_exception{Regexp.new("a", 0, "")}


/c(.)t/ =~ 'cat'                    
test_equal MatchData, Regexp.last_match.class
test_equal 'cat', Regexp.last_match(0)
test_equal 'a', Regexp.last_match(1)
test_equal nil, Regexp.last_match(2)

x = ["fb"]
poo = /^f(.+)$/
test_equal ['b'], x.grep(poo){|z| Regexp.last_match(1)}

x = "fb".."fc"
poo = /^f(.+)$/
test_equal ['b','c'], x.grep(poo){|z| Regexp.last_match(1)}

# JRUBY-2318
def f &block
  re = Regexp.new( "^lib\\/(.*)\\.rb$" )
  yield re
end

fn = "lib/foo.rb"

f { |re|
  fn =~ re
  test_equal "lib/foo.rb", Regexp.last_match.to_s
}

test_equal "lib/foo.rb", Regexp.last_match.to_s

# JRUBY-2318
name = [151, 215, 254, 246, 95, 254, 87, 193, 179, 240, 32, 95].pack('C*')
name =~ %r(\A[\[\]]*([^\[\]]+)\]*)
test_no_exception {$1.hash}

# JRUBY-4881
module Yeller
  def holler(); "hey, look"; end
end

str = "one_two"
str.extend(Yeller)
str =~ /(one)_(two)/

test_exception(NoMethodError) { $1.holler }
test_exception(NoMethodError) { $2.holler }