require 'test/minirunit'
test_check "Test string:"

#########    test1   #################
testcase='toto'
test_ok(1 == idx = testcase.index('o'))
test_ok(3 == testcase.index('o',idx.succ))
test_ok(3 == idx = testcase.rindex('o'))
test_ok(1 == testcase.rindex('o', idx-1))

test_equal(2, "\001\001\000".sum)

test_equal(["1", "2", "3"], "1x2x3".split('x'))
test_equal(["1", "2", "3"], "1   2     3".split(' '))
test_equal(["1", "2", "3"], "  1   2     3".split(' '))
# explicit Regex will not ignore leading whitespace
test_equal(["", "1", "2", "3"], "  1   2     3".split(/\s+/))
test_equal(["1", "2", "3"], "1 2 3".split())
test_equal(["1", "2", "3"], "1x2y3".split(/x|y/))
test_equal(["1", "x", "2", "y", "3"], "1x2y3".split(/(x|y)/))
test_equal(["1", "x", "a", "2", "x", "a", "3"], "1xta2xta3".split(/(x)t(.)/))
test_equal(["foo"], "foo".split("whatever", 1))
test_equal(["", "a", "b", "c"], "/a/b/c".split("/"))
test_equal(["a", "b", "c"], "abc".split(//))


test_equal("hihihi", "hi" * 3)

s = "foobar"
s["foo"] = "baz"
test_equal("bazbar", s)
s["foo"] = "xyz"
test_equal("bazbar", s)

test_equal(9, "alphabetagamma" =~ /gamma$/)
test_equal(nil, "alphabetagamma" =~ /GAMMA$/)

test_equal("Hello", "hello".capitalize)
test_equal("123abc", "123ABC".capitalize)

s ="hello"
s.capitalize!
test_equal("Hello", s)
test_equal(nil, s.capitalize!)
s = "123ABC"
s.capitalize!
test_equal("123abc", s)
test_equal(nil, s.capitalize!)

test_equal("hello", "hello\n".chomp)
test_equal("hello", "hello".chomp)
test_equal("", "".chomp)
test_equal("", "x".chomp("x"))
test_equal("x", "x".chomp("xx"))
old_separator = $/
$/ = 'x'
test_equal("hello", "hellox".chomp)
test_equal("hello", "hello".chomp)
$/ = old_separator

test_equal("hello", "HELlo".downcase)
s = "HELlo"
test_equal("hello", s.downcase!)
test_equal(nil, s.downcase!)

test_equal("HELLO", "HELlo".upcase)
s = "HeLLo"
test_equal("HELLO", s.upcase!)
test_equal(nil, s.upcase!)

test_equal(["cruel", "world"], "cruel world".scan(/\w+/))