require 'test/minirunit'

test_check "string & char"

test_ok("abcd" == "abcd")
test_ok("abcd" === "abcd")
# compile time string concatenation
test_ok("ab" "cd" == "abcd")
test_equal( "22aacd44", "#{22}aa" "cd#{44}" )
test_equal( "22aacd445566", "#{22}aa" "cd#{44}" "55" "#{66}")
test_ok("abc" !~ /^$/)
test_ok("abc\n" !~ /^$/)
test_ok("abc" !~ /^d*$/)
test_ok(("abc" =~ /d*$/) == 3)
test_equal(0, "" =~ /^$/)
test_equal(0, "\n" =~ /^$/)
test_equal(nil, "a\n" =~ /^$/)
test_equal(2, "a\n\n" =~ /^$/)
test_ok("abcabc" =~ /.*a/ && $& == "abca")
test_ok("abcabc" =~ /.*c/ && $& == "abcabc")
test_ok("abcabc" =~ /.*?a/ && $& == "a")
test_ok("abcabc" =~ /.*?c/ && $& == "abc")
test_ok(/(.|\n)*?\n(b|\n)/ =~ "a\nb\n\n" && $& == "a\nb")

test_ok(/^(ab+)+b/ =~ "ababb" && $& == "ababb")
test_ok(/^(?:ab+)+b/ =~ "ababb" && $& == "ababb")
test_ok(/^(ab+)+/ =~ "ababb" && $& == "ababb")
test_ok(/^(?:ab+)+/ =~ "ababb" && $& == "ababb")

test_ok(/(\s+\d+){2}/ =~ " 1 2" && $& == " 1 2")
test_ok(/(?:\s+\d+){2}/ =~ " 1 2" && $& == " 1 2")

$x = <<END;
ABCD
ABCD
END
$x.gsub!(/((.|\n)*?)B((.|\n)*?)D/){$1+$3}
test_ok($x == "AC\nAC\n")

test_ok("foobar" =~ /foo(?=(bar)|(baz))/)
test_ok("foobaz" =~ /foo(?=(bar)|(baz))/)

$foo = "abc"
test_ok("#$foo = abc" == "abc = abc")
test_ok("#{$foo} = abc" == "abc = abc")

foo = "abc"
test_ok("#{foo} = abc" == "abc = abc")

test_ok('-' * 5 == '-----')
test_ok('-' * 1 == '-')
test_ok('-' * 0 == '')

foo = '-'
test_ok(foo * 5 == '-----')
test_ok(foo * 1 == '-')
test_ok(foo * 0 == '')

$x = "a.gif"
test_ok($x.sub(/.*\.([^\.]+)$/, '\1') == "gif")
test_ok($x.sub(/.*\.([^\.]+)$/, 'b.\1') == "b.gif")
test_ok($x.sub(/.*\.([^\.]+)$/, '\2') == "")
test_ok($x.sub(/.*\.([^\.]+)$/, 'a\2b') == "ab")
test_ok($x.sub(/.*\.([^\.]+)$/, '<\&>') == "<a.gif>")

# character constants(assumes ASCII)
test_ok("a"[0] == ?a)
test_ok(?a == ?a)
test_ok(?\C-a == 1)
test_ok(?\M-a == 225)
test_ok(?\M-\C-a == 129)
test_ok("a".upcase![0] == ?A)
test_ok("A".downcase![0] == ?a)
test_ok("abc".tr!("a-z", "A-Z") == "ABC")
test_ok("aabbcccc".tr_s!("a-z", "A-Z") == "ABC")
test_ok("abcc".squeeze!("a-z") == "abc")
test_ok("abcd".delete!("bc") == "ad")

$x = "abcdef"
$y = [ ?a, ?b, ?c, ?d, ?e, ?f ]
$bad = false
$x.each_byte {|i|
  if i != $y.shift
    $bad = true
    break
  end
}
test_ok(!$bad)

s = "a string"
s[0..s.size]="another string"
test_ok(s == "another string")


