require 'test/unit'

#
# The tests in this file are written in terms of "stringlike" objects
# instead of using the class "String" directly. The purpose of this is
# to make it possible to test other stringlike classes too (a simple
# example would be to test a subclass of "String", i.e. how the
# String-class behaves when sub-classed).
#
# This flexibility has not been used yet, but will soon ...
#
# It is implemented by having the member variable @cls initialized to
# the stringlike class we want to test. The S() utility method (used
# "everywhere" in this file) converts String objects to stringlike
# objects of the class @cls.
#
# This file will probably need to splitted again when another class
# is tested too. Something like "TestString.rb", "TestStringSubclass.rb"
# and "StringBase.rb".
#

class TestString < Test::Unit::TestCase

  def initialize(*args)
    @cls = String

    begin
      S("Foo")[/./, 1]
      @aref_re_nth = true
    rescue
      @aref_re_nth = false
    end
    begin
      S("Foo")[/Bar/] = S("")
      @aref_re_silent = true
    rescue
      @aref_re_silent = false
    end
    begin
      S("Foo").slice!(4)
      @aref_slicebang_silent = true
    rescue
      @aref_slicebang_silent = false
    end
    super
  end

  # The "S" method converts String objects into objects of the
  # stringlike class given in the variable @cls. The parameter to
  # "S" may be either a single String, or an array of Strings.
  #
  def S(str_or_array)
    if str_or_array.instance_of?(Array)
      str_or_array.map {|str| @cls.new(str) }
    else
      @cls.new(str_or_array)
    end
  end
    
  def test_AREF # '[]'
    assert_equal(65,  S("AooBar")[0])
    assert_equal(66,  S("FooBaB")[-1])
    assert_equal(nil, S("FooBar")[6])
    assert_equal(nil, S("FooBar")[-7])

    assert_equal(S("Foo"), S("FooBar")[0,3])
    assert_equal(S("Bar"), S("FooBar")[-3,3])
    assert_equal(S(""),    S("FooBar")[6,2])
    assert_equal(nil,      S("FooBar")[-7,10])

    assert_equal(S("Foo"), S("FooBar")[0..2])
    assert_equal(S("Foo"), S("FooBar")[0...3])
    assert_equal(S("Bar"), S("FooBar")[-3..-1])
    assert_equal("",      S("FooBar")[6..2])
    assert_equal(nil,      S("FooBar")[-10..-7])

    assert_equal(S("Foo"), S("FooBar")[/^F../])
    assert_equal(S("Bar"), S("FooBar")[/..r$/])
    assert_equal(nil,      S("FooBar")[/xyzzy/])
    assert_equal(nil,      S("FooBar")[/plugh/])

    assert_equal(S("Foo"), S("FooBar")[S("Foo")])
    assert_equal(S("Bar"), S("FooBar")[S("Bar")])
    assert_equal(nil,      S("FooBar")[S("xyzzy")])
    assert_equal(nil,      S("FooBar")[S("plugh")])

    if @aref_re_nth
      assert_equal(S("Foo"), S("FooBar")[/([A-Z]..)([A-Z]..)/, 1])
      assert_equal(S("Bar"), S("FooBar")[/([A-Z]..)([A-Z]..)/, 2])
      assert_equal(nil,      S("FooBar")[/([A-Z]..)([A-Z]..)/, 3])
      assert_equal(S("Bar"), S("FooBar")[/([A-Z]..)([A-Z]..)/, -1])
      assert_equal(S("Foo"), S("FooBar")[/([A-Z]..)([A-Z]..)/, -2])
      assert_equal(nil,      S("FooBar")[/([A-Z]..)([A-Z]..)/, -3])
    end
  end

  def test_ASET # '[]='
    s = S("FooBar")
    s[0] = S('A')
    assert_equal(S("AooBar"), s)

    s[-1]= S('B')
    assert_equal(S("AooBaB"), s)
    assert_raise(IndexError) { s[-7] = S("xyz") }
    assert_equal(S("AooBaB"), s)
    s[0] = S("ABC")
    assert_equal(S("ABCooBaB"), s)

    s = S("FooBar")
    s[0,3] = S("A")
    assert_equal(S("ABar"),s)
    s[0] = S("Foo")
    assert_equal(S("FooBar"), s)
    s[-3,3] = S("Foo")
    assert_equal(S("FooFoo"), s)
    assert_raise(IndexError) { s[7,3] =  S("Bar") }
    assert_raise(IndexError) { s[-7,3] = S("Bar") }

    s = S("FooBar")
    s[0..2] = S("A")
    assert_equal(S("ABar"), s)
    s[1..3] = S("Foo")
    assert_equal(S("AFoo"), s)
    s[-4..-4] = S("Foo")
    assert_equal(S("FooFoo"), s)
    assert_raise(RangeError) { s[7..10]   = S("Bar") }
    assert_raise(RangeError) { s[-7..-10] = S("Bar") }

    s = S("FooBar")
    s[/^F../]= S("Bar")
    assert_equal(S("BarBar"), s)
    s[/..r$/] = S("Foo")
    assert_equal(S("BarFoo"), s)
    if @aref_re_silent
      s[/xyzzy/] = S("None")
      assert_equal(S("BarFoo"), s)
    else
      assert_raise(IndexError) { s[/xyzzy/] = S("None") }
    end
    if @aref_re_nth
      s[/([A-Z]..)([A-Z]..)/, 1] = S("Foo")
      assert_equal(S("FooFoo"), s)
      s[/([A-Z]..)([A-Z]..)/, 2] = S("Bar")
      assert_equal(S("FooBar"), s)
      assert_raise(IndexError) { s[/([A-Z]..)([A-Z]..)/, 3] = "None" }
      s[/([A-Z]..)([A-Z]..)/, -1] = S("Foo")
      assert_equal(S("FooFoo"), s)
      s[/([A-Z]..)([A-Z]..)/, -2] = S("Bar")
      assert_equal(S("BarFoo"), s)
      assert_raise(IndexError) { s[/([A-Z]..)([A-Z]..)/, -3] = "None" }
    end

    s = S("FooBar")
    s[S("Foo")] = S("Bar")
    assert_equal(S("BarBar"), s)

    s = S("a string")
    s[0..s.size] = S("another string")
    assert_equal(S("another string"), s)
  end

  def test_CMP # '<=>'
    assert_equal(1, S("abcdef") <=> S("abcde"))
    assert_equal(0, S("abcdef") <=> S("abcdef"))
    assert_equal(-1, S("abcde") <=> S("abcdef"))

    assert_equal(-1, S("ABCDEF") <=> S("abcdef"))
  end

  def test_EQUAL # '=='
    assert_equal(false, S("foo") == :foo)
    assert(S("abcdef") == S("abcdef"))

    assert(S("CAT") != S('cat'))
    assert(S("CaT") != S('cAt'))
  end

  def test_LSHIFT # '<<'
    assert_equal(S("world!"), S("world") << 33)
    assert_equal(S("world!"), S("world") << S('!'))
  end

  # Helper class to test String#=~.
  #
  class MatchDefiner
    def initialize(result)
      @result = result
    end

    def =~(other)
      [other, @result]
    end
  end

  def test_MATCH # '=~'
    # "str =~ regexp" same as "regexp =~ str"
    assert_equal(10,  S("FeeFieFoo-Fum") =~ /Fum$/)
    assert_equal(nil, S("FeeFieFoo-Fum") =~ /FUM$/)

    # "str =~ obj" calls  "obj =~ str"
    assert_equal(["aaa",  123],  "aaa" =~ MatchDefiner.new(123))
    assert_equal(["bbb", :foo],  "bbb" =~ MatchDefiner.new(:foo))
    assert_equal(["ccc",  nil],  "ccc" =~ MatchDefiner.new(nil))

    # default Object#=~ method.
    assert_equal(false,  "a string" =~ Object.new)
  end

  def test_MOD # '%'
    assert_equal(S("00123"), S("%05d") % 123)
    assert_equal(S("123  |00000001"), S("%-5s|%08x") % [123, 1])
    x = S("%3s %-4s%%foo %.0s%5d %#x%c%3.1f %b %x %X %#b %#x %#X") %
    [S("hi"),
      123,
      S("never seen"),
      456,
      0,
      ?A,
      3.0999,
      11,
      171,
      171,
      11,
      171,
      171]

    assert_equal(S(' hi 123 %foo   456 0x0A3.1 1011 ab AB 0b1011 0xab 0XAB'), x)
  end

  def test_MUL # '*'
    assert_equal(S("XXX"),  S("X") * 3)
    assert_equal(S("HOHO"), S("HO") * 2)
  end

  def test_PLUS # '+'
    assert_equal(S("Yodel"), S("Yo") + S("del"))
  end

  def casetest(a, b, rev=false)
    case a
      when b
        assert(!rev)
      else
        assert(rev)
    end
  end

  def test_VERY_EQUAL # '==='
    assert_equal(false, S("foo") === :foo)
    casetest(S("abcdef"), S("abcdef"))

    casetest(S("CAT"), S('cat'), true) # Reverse the test - we don't want to
    casetest(S("CaT"), S('cAt'), true) # find these in the case.
  end

  def test_capitalize
    assert_equal(S("Hello"),  S("hello").capitalize)
    assert_equal(S("Hello"),  S("hELLO").capitalize)
    assert_equal(S("123abc"), S("123ABC").capitalize)
  end

  def test_capitalize!
    a = S("hello"); a.capitalize!
    assert_equal(S("Hello"), a)

    a = S("hELLO"); a.capitalize!
    assert_equal(S("Hello"), a)

    a = S("123ABC"); a.capitalize!
    assert_equal(S("123abc"), a)

    assert_equal(nil,         S("123abc").capitalize!)
    assert_equal(S("123abc"), S("123ABC").capitalize!)
    assert_equal(S("Abc"),    S("ABC").capitalize!)
    assert_equal(S("Abc"),    S("abc").capitalize!)
    assert_equal(nil,         S("Abc").capitalize!)

    a = S("hello")
    b = a.dup
    assert_equal(S("Hello"), a.capitalize!)
    assert_equal(S("hello"), b)
   
  end

    def test_casecmp
      # 0
      assert_equal(0, S("123abc").casecmp(S("123ABC")))
      assert_equal(0, S("123AbC").casecmp(S("123aBc")))
      assert_equal(0, S("123ABC").casecmp(S("123ABC")))
      # 1
      assert_equal(1, S("1X3ABC").casecmp(S("123ABC")))
      assert_equal(1, S("123AXC").casecmp(S("123ABC")))
      assert_equal(1, S("123ABX").casecmp(S("123ABC")))
      assert_equal(1, S("123ABCX").casecmp(S("123ABC")))
      # -1
      assert_equal(-1, S("1#3ABC").casecmp(S("123ABC")))
      assert_equal(-1, S("123A#C").casecmp(S("123ABC")))
      assert_equal(-1, S("123AB#").casecmp(S("123ABC")))
      assert_equal(-1, S("123AB").casecmp(S("123ABC")))
    end

  def test_center
    assert_equal(S("hello"),       S("hello").center(4))
    assert_equal(S("   hello   "), S("hello").center(11))

    # with explicit padding
    assert_equal(S("1111hi11111"), S("hi").center(11, "1"))
    assert_equal(S("1212hi12121"), S("hi").center(11, "12"))
    assert_equal(S("1234hi12341"), S("hi").center(11, "1234"))

    # zero width padding
    assert_raise(ArgumentError)  { S("hi").center(11, "") }
  end

  def test_chomp
    assert_equal(S("hello"), S("hello").chomp("\n"))
    assert_equal(S("hello"), S("hello\n").chomp("\n"))

    $/ = "\n"

    assert_equal(S("hello"), S("hello").chomp)
    assert_equal(S("hello"), S("hello\n").chomp)

    $/ = "!"
    assert_equal(S("hello"), S("hello").chomp)
    assert_equal(S("hello"), S("hello!").chomp)
    $/ = "\n"
  end

  def test_chomp!
    a = S("hello")
    a.chomp!(S("\n"))

    assert_equal(S("hello"), a)
    assert_equal(nil, a.chomp!(S("\n")))

    a = S("hello\n")
    a.chomp!(S("\n"))
    assert_equal(S("hello"), a)

    $/ = "\n"
    a = S("hello")
    a.chomp!
    assert_equal(S("hello"), a)

    a = S("hello\n")
    a.chomp!
    assert_equal(S("hello"), a)

    $/ = "!"
    a = S("hello")
    a.chomp!
    assert_equal(S("hello"), a)

    a="hello!"
    a.chomp!
    assert_equal(S("hello"), a)

    $/ = "\n"

    a = S("hello\n")
    b = a.dup
    assert_equal(S("hello"), a.chomp!)
    assert_equal(S("hello\n"), b)
   
  end

  def test_chop
    assert_equal(S("hell"),    S("hello").chop)
    assert_equal(S("hello"),   S("hello\r\n").chop)
    assert_equal(S("hello\n"), S("hello\n\r").chop)
    assert_equal(S(""),        S("\r\n").chop)
    assert_equal(S(""),        S("").chop)
  end

  def test_chop!
    a = S("hello").chop!
    assert_equal(S("hell"), a)

    a = S("hello\r\n").chop!
    assert_equal(S("hello"), a)

    a = S("hello\n\r").chop!
    assert_equal(S("hello\n"), a)

    a = S("\r\n").chop!
    assert_equal(S(""), a)

    a = S("").chop!
    assert_nil(a)

    a = S("hello\n")
    b = a.dup
    assert_equal(S("hello"),   a.chop!)
    assert_equal(S("hello\n"), b)
  end

  def test_clone
    for taint in [ false, true ]
      for frozen in [ false, true ]
        a = S("Cool")
        a.taint  if taint
        a.freeze if frozen
        b = a.clone

        assert_equal(a, b)
        assert(a.__id__ != b.__id__)
        assert_equal(a.frozen?, b.frozen?)
        assert_equal(a.tainted?, b.tainted?)
      end
    end
  end

  def test_concat
    assert_equal(S("world!"), S("world").concat(33))
    assert_equal(S("world!"), S("world").concat(S('!')))
  end

  def test_count
    a = S("hello world")
    assert_equal(5, a.count(S("lo")))
    assert_equal(2, a.count(S("lo"), S("o")))
    assert_equal(4, a.count(S("hello"), S("^l")))
    assert_equal(4, a.count(S("ej-m")))
  end

  def test_crypt
    assert_equal(S('aaGUC/JkO9/Sc'), S("mypassword").crypt(S("aa")))
    assert(S('aaGUC/JkO9/Sc') != S("mypassword").crypt(S("ab")))

    # "salt" should be at least 2 characters
    assert_raise(ArgumentError) { S("mypassword").crypt("a")}
  end

  def test_delete
    assert_equal(S("heo"),  S("hello").delete(S("l"), S("lo")))
    assert_equal(S("he"),   S("hello").delete(S("lo")))
    assert_equal(S("hell"), S("hello").delete(S("aeiou"), S("^e")))
    assert_equal(S("ho"),   S("hello").delete(S("ej-m")))
  end

  def test_delete!
    a = S("hello")
    a.delete!(S("l"), S("lo"))
    assert_equal(S("heo"), a)

    a = S("hello")
    a.delete!(S("lo"))
    assert_equal(S("he"), a)

    a = S("hello")
    a.delete!(S("aeiou"), S("^e"))
    assert_equal(S("hell"), a)

    a = S("hello")
    a.delete!(S("ej-m"))
    assert_equal(S("ho"), a)

    a = S("hello")
    assert_nil(a.delete!(S("z")))

    a = S("hello")
    b = a.dup
    a.delete!(S("lo"))
    assert_equal(S("he"), a)
    assert_equal(S("hello"), b)
  end


  def test_downcase
    assert_equal(S("hello"), S("helLO").downcase)
    assert_equal(S("hello"), S("hello").downcase)
    assert_equal(S("hello"), S("HELLO").downcase)
    assert_equal(S("abc hello 123"), S("abc HELLO 123").downcase)
  end

  def test_downcase!
    a = S("helLO")
    b = a.dup
    assert_equal(S("hello"), a.downcase!)
    assert_equal(S("hello"), a)
    assert_equal(S("helLO"), b)

    a=S("hello")
    assert_nil(a.downcase!)
    assert_equal(S("hello"), a)
  end

  def test_dump
    a= S("Test") << 1 << 2 << 3 << 9 << 13 << 10
    assert_equal(S('"Test\\001\\002\\003\\t\\r\\n"'), a.dump)
  end

  def test_dup
    for taint in [ false, true ]
      for frozen in [ false, true ]
        a = S("hello")
        a.taint  if taint
        a.freeze if frozen
        b = a.dup 

        assert_equal(a, b)
        assert(a.__id__ != b.__id__)
        assert(!b.frozen?)
        assert_equal(a.tainted?, b.tainted?)
      end
    end     
  end

  def test_each
    $/ = "\n"
    res=[]
    S("hello\nworld").each {|x| res << x}
    assert_equal(S("hello\n"), res[0])
    assert_equal(S("world"),   res[1])

    res=[]
    S("hello\n\n\nworld").each(S('')) {|x| res << x}
    assert_equal(S("hello\n\n\n"), res[0])
    assert_equal(S("world"),       res[1])

    $/ = "!"
    res=[]
    S("hello!world").each {|x| res << x}
    assert_equal(S("hello!"), res[0])
    assert_equal(S("world"),  res[1])

    $/ = "\n"
  end

  def test_each_byte
    res = []
    S("ABC").each_byte {|x| res << x }
    assert_equal(65, res[0])
    assert_equal(66, res[1])
    assert_equal(67, res[2])
  end

  def test_each_line
    $/ = "\n"
    res=[]
    S("hello\nworld").each {|x| res << x}
    assert_equal(S("hello\n"), res[0])
    assert_equal(S("world"),   res[1])

    res=[]
    S("hello\n\n\nworld").each(S('')) {|x| res << x}
    assert_equal(S("hello\n\n\n"), res[0])
    assert_equal(S("world"),       res[1])

    $/ = "!"
    res=[]
    S("hello!world").each {|x| res << x}
    assert_equal(S("hello!"), res[0])
    assert_equal(S("world"),  res[1])

    $/ = "\n"
  end

  def test_empty?
    assert(S("").empty?)
    assert(!S("not").empty?)
  end

  def test_eql?
    a = S("hello")
    assert(a.eql?(S("hello")))
    assert(a.eql?(a))
  end

  def test_gsub
    assert_equal(S("h*ll*"),     S("hello").gsub(/[aeiou]/, S('*')))
    assert_equal(S("h<e>ll<o>"), S("hello").gsub(/([aeiou])/, S('<\1>')))
    assert_equal(S("104 101 108 108 111 "),
                 S("hello").gsub(/./) { |s| s[0].to_s + S(' ')})
    assert_equal(S("HELL-o"), 
                 S("hello").gsub(/(hell)(.)/) { |s| $1.upcase + S('-') + $2 })

    a = S("hello")
    a.taint
    assert(a.gsub(/./, S('X')).tainted?)
  end

  def test_gsub!
    a = S("hello")
    b = a.dup
    a.gsub!(/[aeiou]/, S('*'))
    assert_equal(S("h*ll*"), a)
    assert_equal(S("hello"), b)

    a = S("hello")
    a.gsub!(/([aeiou])/, S('<\1>'))
    assert_equal(S("h<e>ll<o>"), a)

    a = S("hello")
    a.gsub!(/./) { |s| s[0].to_s + S(' ')}
    assert_equal(S("104 101 108 108 111 "), a)

    a = S("hello")
    a.gsub!(/(hell)(.)/) { |s| $1.upcase + S('-') + $2 }
    assert_equal(S("HELL-o"), a)

    r = S('X')
    r.taint
    a.gsub!(/./, r)
    assert(a.tainted?) 

    a = S("hello")
    assert_nil(a.sub!(S('X'), S('Y')))
  end

  def test_hash
    assert_equal(S("hello").hash, S("hello").hash)
    assert(S("hello").hash != S("helLO").hash)
  end

  def test_hex
    assert_equal(255,  S("0xff").hex)
    assert_equal(-255, S("-0xff").hex)
    assert_equal(255,  S("ff").hex)
    assert_equal(-255, S("-ff").hex)
    assert_equal(0,    S("-ralph").hex)
    assert_equal(-15,  S("-fred").hex)
    assert_equal(15,   S("fred").hex)
  end

  def test_include?
    assert( S("foobar").include?(?f))
    assert( S("foobar").include?(S("foo")))
    assert(!S("foobar").include?(S("baz")))
    assert(!S("foobar").include?(?z))
  end

  def test_index
    assert_equal(0, S("hello").index(?h))
    assert_equal(1, S("hello").index(S("ell")))
    assert_equal(2, S("hello").index(/ll./))

    assert_equal(3, S("hello").index(?l, 3))
    assert_equal(3, S("hello").index(S("l"), 3))
    assert_equal(3, S("hello").index(/l./, 3))

    assert_nil(S("hello").index(?z, 3))
    assert_nil(S("hello").index(S("z"), 3))
    assert_nil(S("hello").index(/z./, 3))

    assert_nil(S("hello").index(?z))
    assert_nil(S("hello").index(S("z")))
    assert_nil(S("hello").index(/z./))
  end

    def test_insert
      assert_equal(S("BCAD"), S("AD").insert(0, S("BC")))
      assert_equal(S("ABCD"), S("AD").insert(1, S("BC")))
      assert_equal(S("ADBC"), S("AD").insert(2, S("BC")))
      assert_raise(IndexError) { S("AD").insert(3, S("BC")) }
      assert_equal(S("ADBC"), S("AD").insert(-1, S("BC")))
      assert_equal(S("ABCD"), S("AD").insert(-2, S("BC")))
      assert_equal(S("BCAD"), S("AD").insert(-3, S("BC")))
      assert_raise(IndexError) { S("AD").insert(-4, S("BC")) }
    end

  def generic_test_intern(method)
    assert_equal(:koala, S("koala").send(method))
    assert(:koala !=     S("Koala").send(method))

    # Verify that different types of strings are allowed,
    # and do it without using the new literal syntax
    # for symbols (i.e. :"with spaces").
    #
    for str in ["identifier", "with spaces", "9with_digits", "9and spaces"]
      sym = S(str).send(method)
      assert_instance_of(Symbol, sym)
      assert_equal(str, sym.to_s)
    end

    # error cases
      assert_raise(ArgumentError) { S("").send(method) }
    assert_raise(ArgumentError) { S("with\0null\0inside").send(method) }
  end

  def test_intern
    generic_test_intern(:intern)
  end

  def test_length
    assert_equal(0, S("").length)
    assert_equal(4, S("1234").length)
    assert_equal(6, S("1234\r\n").length)
    assert_equal(7, S("\0011234\r\n").length)
  end

  def test_ljust
    assert_equal(S("hello"),       S("hello").ljust(4))
    assert_equal(S("hello      "), S("hello").ljust(11))

    # with explicit padding
    assert_equal(S("hi111111111"), S("hi").ljust(11, "1"))
    assert_equal(S("hi121212121"), S("hi").ljust(11, "12"))
    assert_equal(S("hi123412341"), S("hi").ljust(11, "1234"))

    # zero width padding
      assert_raise(ArgumentError)  { S("hello").ljust(11, "") }
  end

    def test_lstrip
      a = S("  hello")
      assert_equal(S("hello"), a.lstrip)
      assert_equal(S("  hello"), a)
      assert_equal(S("hello "), S(" hello ").lstrip)
      assert_equal(S("hello"), S("hello").lstrip)
    end
    def test_lstrip!
      a = S("  abc")
      b = a.dup
      assert_equal(S("abc"), a.lstrip!)
      assert_equal(S("abc"), a)
      assert_equal(S("  abc"), b)
    end

    def test_match
      a = S("cruel world")
      m = a.match(/\w+/)
      assert_kind_of(MatchData, m)
      assert_equal(S("cruel"), m.to_s)

      m = a.match('\w+')
      assert_kind_of(MatchData, m)
      assert_equal(S("cruel"), m.to_s)
    end

  def test_next
    assert_equal(S("abd"), S("abc").next)
    assert_equal(S("z"),   S("y").next)
    assert_equal(S("aaa"), S("zz").next)

    assert_equal(S("124"),  S("123").next)
    assert_equal(S("1000"), S("999").next)

    assert_equal(S("2000aaa"),  S("1999zzz").next)
    assert_equal(S("AAAAA000"), S("ZZZZ999").next)

    assert_equal(S("*+"), S("**").next)
  end

  def test_next!
    a = S("abc")
    b = a.dup
    assert_equal(S("abd"), a.next!)
    assert_equal(S("abd"), a)
    assert_equal(S("abc"), b)

    a = S("y")
    assert_equal(S("z"), a.next!)
    assert_equal(S("z"), a)

    a = S("zz")
    assert_equal(S("aaa"), a.next!)
    assert_equal(S("aaa"), a)

    a = S("123")
    assert_equal(S("124"), a.next!)
    assert_equal(S("124"), a)

    a = S("999")
    assert_equal(S("1000"), a.next!)
    assert_equal(S("1000"), a)

    a = S("1999zzz")
    assert_equal(S("2000aaa"), a.next!)
    assert_equal(S("2000aaa"), a)

    a = S("ZZZZ999")
    assert_equal(S("AAAAA000"), a.next!)
    assert_equal(S("AAAAA000"), a)

    a = S("**")
    assert_equal(S("*+"), a.next!)
    assert_equal(S("*+"), a)
  end

  def test_oct
    assert_equal(255,  S("0377").oct)
    assert_equal(255,  S("377").oct)
    assert_equal(-255, S("-0377").oct)
    assert_equal(-255, S("-377").oct)
    assert_equal(0,    S("OO").oct)
    assert_equal(24,   S("030OO").oct)
  end

  def test_replace
    a = S("foo")
    assert_equal(S("f"), a.replace(S("f")))

    a = S("foo")
    assert_equal(S("foobar"), a.replace(S("foobar")))

    a = S("foo")
    a.taint
    b = a.replace(S("xyz"))
    assert_equal(S("xyz"), b)
    assert(b.tainted?)
  end

  def test_reverse
    assert_equal(S("beta"), S("ateb").reverse)
    assert_equal(S("madamImadam"), S("madamImadam").reverse)

    a=S("beta")
    assert_equal(S("ateb"), a.reverse)
    assert_equal(S("beta"), a)
  end

  def test_reverse!
    a = S("beta")
    b = a.dup
    assert_equal(S("ateb"), a.reverse!)
    assert_equal(S("ateb"), a)
    assert_equal(S("beta"), b)

    assert_equal(S("madamImadam"), S("madamImadam").reverse!)

    a = S("madamImadam")
    assert_equal(S("madamImadam"), a.reverse!)  # ??
    assert_equal(S("madamImadam"), a)
  end

  def test_rindex
    assert_equal(3, S("hello").rindex(?l))
    assert_equal(6, S("ell, hello").rindex(S("ell")))
    assert_equal(7, S("ell, hello").rindex(/ll./))

    assert_equal(3, S("hello,lo").rindex(?l, 3))
    assert_equal(3, S("hello,lo").rindex(S("l"), 3))
    assert_equal(3, S("hello,lo").rindex(/l./, 3))

    assert_nil(S("hello").rindex(?z,     3))
    assert_nil(S("hello").rindex(S("z"), 3))
    assert_nil(S("hello").rindex(/z./,   3))

    assert_nil(S("hello").rindex(?z))
    assert_nil(S("hello").rindex(S("z")))
    assert_nil(S("hello").rindex(/z./))
  end

  def test_rjust
    assert_equal(S("hello"), S("hello").rjust(4))
    assert_equal(S("      hello"), S("hello").rjust(11))

    # with explicit padding
    assert_equal(S("111111111hi"), S("hi").rjust(11, "1"))
    assert_equal(S("121212121hi"), S("hi").rjust(11, "12"))
    assert_equal(S("123412341hi"), S("hi").rjust(11, "1234"))

    # zero width padding
    assert_raise(ArgumentError)  { S("hi").rjust(11, "") }
  end

    def test_rstrip
      a = S("hello  ")
      assert_equal(S("hello"), a.rstrip)
      assert_equal(S("hello  "), a)
      assert_equal(S(" hello"), S(" hello ").rstrip)
      assert_equal(S("hello"), S("hello").rstrip)
    end
    def test_rstrip!
      a = S("abc  ")
      b = a.dup
      assert_equal(S("abc"), a.rstrip!)
      assert_equal(S("abc"), a)
      assert_equal(S("abc  "), b)
    end

  def test_scan
    a = S("cruel world")
    assert_equal([S("cruel"), S("world")],a.scan(/\w+/))
    assert_equal([S("cru"), S("el "), S("wor")],a.scan(/.../))
    assert_equal([[S("cru")], [S("el ")], [S("wor")]],a.scan(/(...)/))

    res = []
    a.scan(/\w+/) { |w| res << w }
    assert_equal([S("cruel"), S("world") ],res)

    res = []
    a.scan(/.../) { |w| res << w }
    assert_equal([S("cru"), S("el "), S("wor")],res)

    res = []
    a.scan(/(...)/) { |w| res << w }
    assert_equal([[S("cru")], [S("el ")], [S("wor")]],res)

    # with a pattern that match empty string
    assert_equal(S(["", "", "1", "", "", "22", "", "", "333", ""]),
                 S("aa1bb22cc333").scan(/\d*/))
  end

  def test_size
    assert_equal(0, S("").size)
    assert_equal(4, S("1234").size)
    assert_equal(6, S("1234\r\n").size)
    assert_equal(7, S("\0011234\r\n").size)
  end

  def test_slice
    assert_equal(65, S("AooBar").slice(0))
    assert_equal(66, S("FooBaB").slice(-1))
    assert_nil(S("FooBar").slice(6))
    assert_nil(S("FooBar").slice(-7))

    assert_equal(S("Foo"), S("FooBar").slice(0,3))
    assert_equal(S(S("Bar")), S("FooBar").slice(-3,3))
    assert_nil(S("FooBar").slice(7,2))     # Maybe should be six?
    assert_nil(S("FooBar").slice(-7,10))

    assert_equal(S("Foo"), S("FooBar").slice(0..2))
    assert_equal(S("Bar"), S("FooBar").slice(-3..-1))
    assert_equal("", S("FooBar").slice(6..2))
    assert_nil(S("FooBar").slice(-10..-7))

    assert_equal(S("Foo"), S("FooBar").slice(/^F../))
    assert_equal(S("Bar"), S("FooBar").slice(/..r$/))
    assert_nil(S("FooBar").slice(/xyzzy/))
    assert_nil(S("FooBar").slice(/plugh/))

    assert_equal(S("Foo"), S("FooBar").slice(S("Foo")))
    assert_equal(S("Bar"), S("FooBar").slice(S("Bar")))
    assert_nil(S("FooBar").slice(S("xyzzy")))
    assert_nil(S("FooBar").slice(S("plugh")))
  end

  def test_slice!
    a = S("AooBar")
    b = a.dup
    assert_equal(65, a.slice!(0))
    assert_equal(S("ooBar"), a)
    assert_equal(S("AooBar"), b)

    a = S("FooBar")
    assert_equal(?r,a.slice!(-1))
    assert_equal(S("FooBa"), a)

    a = S("FooBar")
    if @aref_slicebang_silent
      assert_nil( a.slice!(6) )
    else
      assert_raise(:IndexError) { a.slice!(6) }
    end 
    assert_equal(S("FooBar"), a)

    if @aref_slicebang_silent
      assert_nil( a.slice!(-7) ) 
    else 
      assert_raise(:IndexError) { a.slice!(-7) }
    end
    assert_equal(S("FooBar"), a)

    a = S("FooBar")
    assert_equal(S("Foo"), a.slice!(0,3))
    assert_equal(S("Bar"), a)

    a = S("FooBar")
    assert_equal(S("Bar"), a.slice!(-3,3))
    assert_equal(S("Foo"), a)

    a=S("FooBar")
    if @aref_slicebang_silent
      assert_nil(a.slice!(7,2))      # Maybe should be six?
    else
      assert_raise(:IndexError) {a.slice!(7,2)}     # Maybe should be six?
    end
    assert_equal(S("FooBar"), a)
    if @aref_slicebang_silent
      assert_nil(a.slice!(-7,10))
    else
      assert_raise(:IndexError) {a.slice!(-7,10)}
    end
    assert_equal(S("FooBar"), a)

    a=S("FooBar")
    assert_equal(S("Foo"), a.slice!(0..2))
    assert_equal(S("Bar"), a)

    a=S("FooBar")
    assert_equal(S("Bar"), a.slice!(-3..-1))
    assert_equal(S("Foo"), a)

    a=S("FooBar")
    if @aref_slicebang_silent
      assert_equal("", a.slice!(6..2))
    else
      assert_raise(:RangeError) {a.slice!(6..2)}
    end
    assert_equal(S("FooBar"), a)
    if @aref_slicebang_silent
      assert_nil(a.slice!(-10..-7))
    else
      assert_raise(:RangeError) {a.slice!(-10..-7)}
    end
    assert_equal(S("FooBar"), a)

    a=S("FooBar")
    assert_equal(S("Foo"), a.slice!(/^F../))
    assert_equal(S("Bar"), a)

    a=S("FooBar")
    assert_equal(S("Bar"), a.slice!(/..r$/))
    assert_equal(S("Foo"), a)

    a=S("FooBar")
    if @aref_slicebang_silent
      assert_nil(a.slice!(/xyzzy/))
    else
      assert_raise(:IndexError) {a.slice!(/xyzzy/)}
    end
    assert_equal(S("FooBar"), a)
    if @aref_slicebang_silent
      assert_nil(a.slice!(/plugh/))
    else
      assert_raise(:IndexError) {a.slice!(/plugh/)}
    end
    assert_equal(S("FooBar"), a)

    a=S("FooBar")
    assert_equal(S("Foo"), a.slice!(S("Foo")))
    assert_equal(S("Bar"), a)

    a=S("FooBar")
    assert_equal(S("Bar"), a.slice!(S("Bar")))
    assert_equal(S("Foo"), a)
  end
    
  class MyString < String
  end

  def test_split
    assert_nil($;)
    assert_equal([S("a"), S("b"), S("c")], S(" a   b\t c ").split)
    assert_equal([S("a"), S("b"), S("c")], S(" a   b\t c ").split(S(" ")))

    assert_equal([S(" a "), S(" b "), S(" c ")], S(" a | b | c ").split(S("|")))

    assert_equal([S("a"), S("b"), S("c")], S("aXXbXXcXX").split(/X./))

    assert_equal([S("a"), S("b"), S("c")], S("abc").split(//))

    assert_equal([S("a|b|c")], S("a|b|c").split(S('|'), 1))

    assert_equal([S("a"), S("b|c")], S("a|b|c").split(S('|'), 2))
    assert_equal([S("a"), S("b"), S("c")], S("a|b|c").split(S('|'), 3))

    assert_equal([S("a"), S("b"), S("c"), S("")], S("a|b|c|").split(S('|'), -1))
    assert_equal([S("a"), S("b"), S("c"), S(""), S("")], S("a|b|c||").split(S('|'), -1))

    assert_equal([S("a"), S(""), S("b"), S("c")], S("a||b|c|").split(S('|')))
    assert_equal([S("a"), S(""), S("b"), S("c"), S("")], S("a||b|c|").split(S('|'), -1))
    
    assert_equal(MyString, MyString.new("a|b").split(S('|'))[0].class)
  end

  def test_squeeze
    assert_equal(S("abc"), S("aaabbbbccc").squeeze)
    assert_equal(S("aa bb cc"), S("aa   bb      cc").squeeze(S(" ")))
    assert_equal(S("BxTyWz"), S("BxxxTyyyWzzzzz").squeeze(S("a-z")))
  end

  def test_squeeze!
    a = S("aaabbbbccc")
    b = a.dup
    assert_equal(S("abc"), a.squeeze!)
    assert_equal(S("abc"), a)
    assert_equal(S("aaabbbbccc"), b)

    a = S("aa   bb      cc")
    assert_equal(S("aa bb cc"), a.squeeze!(S(" ")))
    assert_equal(S("aa bb cc"), a)

    a = S("BxxxTyyyWzzzzz")
    assert_equal(S("BxTyWz"), a.squeeze!(S("a-z")))
    assert_equal(S("BxTyWz"), a)

    a=S("The quick brown fox")
    assert_nil(a.squeeze!)
  end

  def test_strip
    assert_equal(S("x"), S("      x        ").strip)
    assert_equal(S("x"), S(" \n\r\t     x  \t\r\n\n      ").strip)
  end

  def test_strip!
    a = S("      x        ")
    b = a.dup
    assert_equal(S("x") ,a.strip!)
    assert_equal(S("x") ,a)
    assert_equal(S("      x        "), b)

    a = S(" \n\r\t     x  \t\r\n\n      ")
    assert_equal(S("x"), a.strip!)
    assert_equal(S("x"), a)

    a = S("x")
    assert_nil(a.strip!)
    assert_equal(S("x") ,a)
  end

  def test_sub
    assert_equal(S("h*llo"),    S("hello").sub(/[aeiou]/, S('*')))
    assert_equal(S("h<e>llo"),  S("hello").sub(/([aeiou])/, S('<\1>')))
    assert_equal(S("104 ello"), S("hello").sub(/./) {
                   |s| s[0].to_s + S(' ')})
    assert_equal(S("HELL-o"),   S("hello").sub(/(hell)(.)/) {
                   |s| $1.upcase + S('-') + $2
                   })

    assert_equal(S("a\\aba"), S("ababa").sub(/b/, '\\'))
    assert_equal(S("ab\\aba"), S("ababa").sub(/(b)/, '\1\\'))
    assert_equal(S("ababa"), S("ababa").sub(/(b)/, '\1'))
    assert_equal(S("ababa"), S("ababa").sub(/(b)/, '\\1'))
    assert_equal(S("a\\1aba"), S("ababa").sub(/(b)/, '\\\1'))
    assert_equal(S("a\\1aba"), S("ababa").sub(/(b)/, '\\\\1'))
    assert_equal(S("a\\baba"), S("ababa").sub(/(b)/, '\\\\\1'))

    assert_equal(S("a--ababababababababab"),
                 S("abababababababababab").sub(/(b)/, '-\9-'))
    assert_equal(S("1-b-0"),
                 S("1b2b3b4b5b6b7b8b9b0").
                 sub(/(b).(b).(b).(b).(b).(b).(b).(b).(b)/, '-\9-'))
    assert_equal(S("1-b-0"),
                 S("1b2b3b4b5b6b7b8b9b0").
                 sub(/(b).(b).(b).(b).(b).(b).(b).(b).(b)/, '-\\9-'))
    assert_equal(S("1-\\9-0"),
                 S("1b2b3b4b5b6b7b8b9b0").
                 sub(/(b).(b).(b).(b).(b).(b).(b).(b).(b)/, '-\\\9-'))
    assert_equal(S("k"),
                 S("1a2b3c4d5e6f7g8h9iAjBk").
                 sub(/.(.).(.).(.).(.).(.).(.).(.).(.).(.).(.).(.)/, '\+'))

    assert_equal(S("ab\\aba"), S("ababa").sub(/b/, '\&\\'))
    assert_equal(S("ababa"), S("ababa").sub(/b/, '\&'))
    assert_equal(S("ababa"), S("ababa").sub(/b/, '\\&'))
    assert_equal(S("a\\&aba"), S("ababa").sub(/b/, '\\\&'))
    assert_equal(S("a\\&aba"), S("ababa").sub(/b/, '\\\\&'))
    assert_equal(S("a\\baba"), S("ababa").sub(/b/, '\\\\\&'))

    a = S("hello")
    a.taint
    assert(a.sub(/./, S('X')).tainted?)
  end

  def test_sub!
    a = S("hello")
    b = a.dup
    a.sub!(/[aeiou]/, S('*'))
    assert_equal(S("h*llo"), a)
    assert_equal(S("hello"), b)

    a = S("hello")
    a.sub!(/([aeiou])/, S('<\1>'))
    assert_equal(S("h<e>llo"), a)

    a = S("hello")
    a.sub!(/./) { |s| s[0].to_s + S(' ')}
    assert_equal(S("104 ello"), a)

    a = S("hello")
    a.sub!(/(hell)(.)/) { |s| $1.upcase + S('-') + $2 }
    assert_equal(S("HELL-o"), a)

    a=S("hello")
    assert_nil(a.sub!(/X/, S('Y')))

    r = S('X')
    r.taint
    a.sub!(/./, r)
    assert(a.tainted?) 
  end

  def test_succ
    assert_equal(S("abd"), S("abc").succ)
    assert_equal(S("z"),   S("y").succ)
    assert_equal(S("aaa"), S("zz").succ)

    assert_equal(S("124"),  S("123").succ)
    assert_equal(S("1000"), S("999").succ)

    assert_equal(S("2000aaa"),  S("1999zzz").succ)
    assert_equal(S("AAAAA000"), S("ZZZZ999").succ)
    assert_equal(S("*+"), S("**").succ)
  end

  def test_succ!
    a = S("abc")
    b = a.dup
    assert_equal(S("abd"), a.succ!)
    assert_equal(S("abd"), a)
    assert_equal(S("abc"), b)

    a = S("y")
    assert_equal(S("z"), a.succ!)
    assert_equal(S("z"), a)

    a = S("zz")
    assert_equal(S("aaa"), a.succ!)
    assert_equal(S("aaa"), a)

    a = S("123")
    assert_equal(S("124"), a.succ!)
    assert_equal(S("124"), a)

    a = S("999")
    assert_equal(S("1000"), a.succ!)
    assert_equal(S("1000"), a)

    a = S("1999zzz")
    assert_equal(S("2000aaa"), a.succ!)
    assert_equal(S("2000aaa"), a)

    a = S("ZZZZ999")
    assert_equal(S("AAAAA000"), a.succ!)
    assert_equal(S("AAAAA000"), a)

    a = S("**")
    assert_equal(S("*+"), a.succ!)
    assert_equal(S("*+"), a)
  end

  def test_sum
    n = S("\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001")
    assert_equal(15, n.sum)
    assert_equal(15, n.sum(32))
    n += S("\001")
    assert_equal(16, n.sum(17))
    assert_equal(16, n.sum(32))
    n[0] = 2
    assert(15 != n.sum)

    # basic test of all reasonable "bit sizes"
    str = S("\xFF" * 257)
    2.upto(32) do |bits|
      assert_equal(65535 % (2**bits), str.sum(bits))
    end

    # with 16 bits the result is modulo 65536
    assert_equal(  255, S("\xFF").sum)
    assert_equal(  510, S("\xFF\xFF").sum)
    assert_equal(65535, S("\xFF" * 257).sum)
    assert_equal(  254, S("\xFF" * 258).sum)

    # with 32 bits the result is modulo 2**32
    assert_equal(  255, S("\xFF").sum(32))
    assert_equal(  510, S("\xFF\xFF").sum(32))
    assert_equal(65535, S("\xFF" * 257).sum(32))
    assert_equal(65790, S("\xFF" * 258).sum(32))
    # the following case takes 16MB and takes a long time to compute,
    # so it is commented out.
    #assert_equal(  254, S("\xFF" * (257 * 65537 + 1)).sum(32))
  end

  def test_swapcase
    assert_equal(S("hi&LOW"), S("HI&low").swapcase)
  end

  def test_swapcase!
    a = S("hi&LOW")
    b = a.dup
    assert_equal(S("HI&low"), a.swapcase!)
    assert_equal(S("HI&low"), a)
    assert_equal(S("hi&LOW"), b)

    a = S("$^#^%$#!!")
    assert_nil(a.swapcase!)
    assert_equal(S("$^#^%$#!!"), a)
  end

  def test_to_f
    assert_equal(344.3,     S("344.3").to_f)
    assert_equal(5.9742e24, S("5.9742e24").to_f)
    assert_equal(98.6,      S("98.6 degrees").to_f)
    assert_equal(0.0,       S("degrees 100.0").to_f)
  end

  def test_to_i
    assert_equal(1480, S("1480ft/sec").to_i)
    assert_equal(0,    S("speed of sound in water @20C = 1480ft/sec)").to_i)
  end

  def test_to_s
    a = S("me")
    assert_equal("me", a.to_s)
    assert_equal(a.__id__, a.to_s.__id__) if @cls == String
  end

  def test_to_str
    a = S("me")
    assert_equal("me", a.to_s)
    assert_equal(a.__id__, a.to_s.__id__) if @cls == String
  end

    def test_to_sym
      generic_test_intern(:to_sym)
    end

  def test_tr
    assert_equal(S("hippo"), S("hello").tr(S("el"), S("ip")))
    assert_equal(S("*e**o"), S("hello").tr(S("^aeiou"), S("*")))
    assert_equal(S("hal"),   S("ibm").tr(S("b-z"), S("a-z")))
  end

  def test_tr!
    a = S("hello")
    b = a.dup
    assert_equal(S("hippo"), a.tr!(S("el"), S("ip")))
    assert_equal(S("hippo"), a)
    assert_equal(S("hello"),b)

    a = S("hello")
    assert_equal(S("*e**o"), a.tr!(S("^aeiou"), S("*")))
    assert_equal(S("*e**o"), a)

    a = S("IBM")
    assert_equal(S("HAL"), a.tr!(S("B-Z"), S("A-Z")))
    assert_equal(S("HAL"), a)

    a = S("ibm")
    assert_nil(a.tr!(S("B-Z"), S("A-Z")))
    assert_equal(S("ibm"), a)
  end

  def test_tr_s
    assert_equal(S("hypo"), S("hello").tr_s(S("el"), S("yp")))
    assert_equal(S("h*o"),  S("hello").tr_s(S("el"), S("*")))
  end

  def test_tr_s!
    a = S("hello")
    b = a.dup
    assert_equal(S("hypo"),  a.tr_s!(S("el"), S("yp")))
    assert_equal(S("hypo"),  a)
    assert_equal(S("hello"), b)

    a = S("hello")
    assert_equal(S("h*o"), a.tr_s!(S("el"), S("*")))
    assert_equal(S("h*o"), a)
  end

  def test_upcase
    assert_equal(S("HELLO"), S("hello").upcase)
    assert_equal(S("HELLO"), S("hello").upcase)
    assert_equal(S("HELLO"), S("HELLO").upcase)
    assert_equal(S("ABC HELLO 123"), S("abc HELLO 123").upcase)
  end

  def test_upcase!
    a = S("hello")
    b = a.dup
    assert_equal(S("HELLO"), a.upcase!)
    assert_equal(S("HELLO"), a)
    assert_equal(S("hello"), b)

    a = S("HELLO")
    assert_nil(a.upcase!)
    assert_equal(S("HELLO"), a)
  end

  def test_upto
    a     = S("aa")
    start = S("aa")
    count = 0
    assert_equal(S("aa"), a.upto(S("zz")) {|s|
                   assert_equal(start, s)
                   start.succ!
                   count += 1
                   })
    assert_equal(676, count)
  end

  def test_s_new
    assert_equal("RUBY", S("RUBY"))
  end
end
