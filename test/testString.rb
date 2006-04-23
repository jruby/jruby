require 'test/minirunit'
test_check "Test string:"

# Generic class to test integer functions.
class IntClass
  def initialize(num); @num = num; end
  def to_int; @num; end; 
end

##### misc #####

test_equal("hihihi", "hi" * 3)
test_equal(9, "alphabetagamma" =~ /gamma$/)
test_equal(nil, "alphabetagamma" =~ /GAMMA$/)
test_equal(false, "foo" == :foo)

##### [] (aref) ######
s = "hello there"

test_equal(101, s[1])
test_equal("ell", s[1, 3])
test_equal("l", s[3, 1])
test_equal("ell", s[1..3])
test_equal("el", s[1...3])
test_equal("er", s[-3, 2])
test_equal("her", s[-4..-2])
# Bug #1467286
#test_equal("", s[-2..-4])
#test_equal("", s[6..2])
test_equal("ell", s[/[aeiow](.)\1/])
test_equal("ell", s[/[aeiow](.)\1/, 0])
test_equal("l", s[/[aeiow](.)\1/, 1])
test_equal(nil, s[/[aeiow](.)\1/, 2])
test_equal("the", s[/(..)e/])
test_equal("th", s[/(..)e/, 1])
test_equal("lo", s["lo"])
test_equal(nil, s["bye"])

##### []= (aset) #######

s = "foobar"
s["foo"] = "baz"
test_equal("bazbar", s)
s[2] = 79
test_equal("baObar", s)

# ""[0,0]="foo" is valid
s = ""
s[0,0]="foo"

test_equal("foo", s)

##### capitalize/capitalize! ######

test_equal("Hello", "hello".capitalize)
test_equal("123abc", "123ABC".capitalize)

s ="hello"
s.capitalize!
test_equal("Hello", s)
test_equal(101, s[IntClass.new(1)])
test_equal(nil, s.capitalize!)
s = "123ABC"
s.capitalize!
test_equal("123abc", s)
test_equal(nil, s.capitalize!)

##### center ######

test_equal("hello", "hello".center(4))
test_equal("       hello        ", "hello".center(20))
test_equal("hello", "hello".center(4, "_-^-"))
test_equal("_-^-_-^helloe_-^-_-^", "helloe".center(20, "_-^-"))
test_equal("-------hello--------", "hello".center(20, "-"))
test_exception(ArgumentError) { "hello".center(11, "") }

##### chomp ######

# See test/testStringChomp.rb

##### chop/chop! ######

test_equal("", "".chop)
test_equal(nil, "".chop!)
test_equal("", "\n".chop)
s = "\n"
test_equal("", s.chop!)
test_equal("", s)

test_equal("string", "string\r\n".chop)
test_equal("string\n", "string\n\r".chop)
test_equal("string", "string\n".chop)
test_equal("strin", "string".chop)
test_equal("", "x".chop.chop)

##### <</concat ######
s = "a"
test_equal("aa", s << "a")
test_equal("aaa", s.concat("a"))
test_equal("aaaa", s << 97)
test_equal("aaaaa", s.concat(97))
test_exception(TypeError) { s << 300 }
test_exception(TypeError) { s.concat(300) }

##### downcase/downcase! ######

test_equal("hello", "HELlo".downcase)
s = "HELlo"
test_equal("hello", s.downcase!)
test_equal(nil, s.downcase!)

##### gsub #####
test_equal("h*ll*", "hello".gsub(/[aeiou]/, '*'))
test_equal("h<e>ll<o>", "hello".gsub(/([aeiou])/, '<\1>'))
test_equal("104 101 108 108 111 ", "hello".gsub(/./) {|s| s[0].to_s + ' '})
test_equal("a-b-c", "a+b+c".gsub("+", "-"))

##### index/rindex ######
testcase='toto'
test_ok(1 == idx = testcase.index('o'))
test_ok(3 == testcase.index('o',idx.succ))
test_ok(3 == idx = testcase.rindex('o'))
test_ok(1 == testcase.rindex('o', idx-1))

##### oct #####
# oct should return zero in appropriate cases
test_equal(0, "b".oct)
test_equal(0, "".oct)

##### replace #####
t = "hello"
s = "world"
s.replace t
test_equal("hello", s)
s.chop!
test_equal("hello", t)

##### reverse/reverse! #####
s = "abc"
test_equal("cba", s.reverse)
test_equal("abc", s)
test_equal("cba", s.reverse!)
test_equal("cba", s)

##### scan

s = "cruel world"
test_equal(["cruel", "world"], s.scan(/\w+/))
test_equal(["cru", "el ", "wor"], s.scan(/.../))
test_equal([["cru"], ["el "], ["wor"]], s.scan(/(...)/))
test_equal([["cr", "ue"], ["l ", "wo"]], s.scan(/(..)(..)/))

l = []
s.scan(/\w+/) { |w| l << "<<#{w}>>" }
test_equal(["<<cruel>>", "<<world>>"], l)
l = ""
s.scan(/(.)(.)/) { |a,b|  l << b; l << a }
test_equal("rceu lowlr", l)

##### split ######

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
test_equal(["/home", "/jruby"], "/home/jruby".split(%r<(?=/)>))
test_equal(["///home", "///jruby"], "///home///jruby".split(%r<(?=///)>))

##### sub #####

test_equal("h*llo", "hello".sub(/[aeiou]/, '*'))
test_equal("h<e>llo", "hello".sub(/([aeiou])/, '<\1>'))
test_equal("104 ello", "hello".sub(/./) {|s| s[0].to_s + ' ' })
test_equal("a-b+c", "a+b+c".sub("+", "-"))
test_equal("a+b+c", "a-b+c".sub("-", "+"))
   
##### succ/succ! #####

def test_succ!(expected, start); start.succ!; test_equal(expected, start); end
test_equal("abce", "abcd".succ)
test_succ!("abce", "abcd")
test_equal("THX1139", "THX1138".succ)
test_succ!("THX1139", "THX1138")
test_equal("<<koalb>>", "<<koala>>".succ)
test_succ!("<<koalb>>", "<<koala>>")
test_equal("2000aaa", "1999zzz".succ)
test_succ!("2000aaa", "1999zzz")
test_equal("AAAA0000", "ZZZ9999".succ)
test_succ!("AAAA0000", "ZZZ9999")
test_equal("**+", "***".succ)
test_succ!("**+", "***")

##### sum #####

test_equal(2, "\001\001\000".sum)

##### swapcase/swapcase! #####

s = "abC"
test_equal("ABc", s.swapcase)
test_equal("abC", s)
test_equal("ABc", s.swapcase!)
test_equal("ABc", s)
s = "111"
test_equal("111", s.swapcase)
test_equal(nil, s.swapcase!)

##### upcase/upcase! ######

test_equal("HELLO", "HELlo".upcase)
s = "HeLLo"
test_equal("HELLO", s.upcase!)
test_equal(nil, s.upcase!)

##### upto ######

UPTO_ANS = ["a8", "a9", "b0"]
s = "a8"
ans = []
s.upto("b0") { |e| ans << e }
test_equal(UPTO_ANS, ans)
test_equal("a8", s)
