require 'test/unit'
require 'rubicon_testcase'

class TestStringSubclass < String; end # for test_to_s/test_to_str

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

class TestString < RubiconTestCase

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
    rescue IndexError
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

  def S(str)
    @cls.new(str)
  end
    
  def casetest(a, b, rev=false) # TODO: rename
    case a
      when b
        assert(!rev)
      else
        assert(rev)
    end
  end

  def test_capitalize
    assert_equal(S("Hello"),  S("hello").capitalize)
    assert_equal(S("Hello"),  S("hELLO").capitalize)
    assert_equal(S("Hello"),  S("Hello").capitalize)
    assert_equal(S("123abc"), S("123ABC").capitalize)
  end

  def test_capitalize_bang
    a = S("hello")
    assert_equal(S("Hello"), a.capitalize!)
    assert_equal(S("Hello"), a)

    a = S("Hello")
    assert_nil a.capitalize!
    assert_equal(S("Hello"), a)

    a = S("hELLO")
    assert_equal S("Hello"), a.capitalize!
    assert_equal S("Hello"), a

    a = S("123ABC")
    assert_equal S("123abc"), a.capitalize!
    assert_equal S("123abc"), a
  end

  def test_capitalize_bang_multibyte
    # TODO: flunk "No capitalize! multibyte tests yet"
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

    assert_raises TypeError do
      'foo'.casecmp Object.new
    end
  end

  def test_center
    s = S("")
    assert_not_equal s.object_id, s.center(0).object_id

    assert_equal S(""),         S("") .center(0)
    assert_equal S("@"),        S("@").center(0)

    assert_equal S(" "),        S("") .center(1)
    assert_equal S("@"),        S("@").center(1)
    assert_equal S("@ "),       S("@").center(2)
    assert_equal S(" @ "),      S("@").center(3)
    assert_equal S(" @  "),     S("@").center(4)
    assert_equal S("  @  "),    S("@").center(5)

    assert_equal S(" @@  "),    S("@@").center(5)

    assert_equal S(""),         S("") .center(0, 'X')
    assert_equal S("@"),        S("@").center(0, 'X')
    assert_equal S("X"),        S("") .center(1, 'X')
    assert_equal S("@"),        S("@").center(1, 'X')
    assert_equal S("@X"),       S("@").center(2, 'X')
    assert_equal S("X@X"),      S("@").center(3, 'X')
    assert_equal S("X@XX"),     S("@").center(4, 'X')
    assert_equal S("XX@XX"),    S("@").center(5, 'X')

    assert_equal S("X@XY"),     S("@").center(4, 'XY')
    assert_equal S("XY@XY"),    S("@").center(5, 'XY')
    assert_equal S("XY@XYX"),   S("@").center(6, 'XY')
    assert_equal S("XYX@XYX"),  S("@").center(7, 'XY')

    assert_raises ArgumentError, "Zero width padding not allowed" do
      S("").center 0, S("")
    end
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

  def test_chomp_bang
    a = S("")
    a.chomp!
    assert_equal('', a)

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
    assert_equal(S(""),        S("").chop)
    assert_equal(S(""),        S("h").chop)
    assert_equal(S("hell"),    S("hello").chop)
    assert_equal(S("hello"),   S("hello\r\n").chop)
    assert_equal(S("hello\n"), S("hello\n\r").chop)
    assert_equal(S(""),        S("\r\n").chop)
  end

  def test_chop_bang
    a = S("").chop!
    assert_nil(a)

    a = S("hello").chop!
    assert_equal(S("hell"), a)

    a = S("hello\r\n").chop!
    assert_equal(S("hello"), a)

    a = S("hello\n\r").chop!
    assert_equal(S("hello\n"), a)

    a = S("\r\n").chop!
    assert_equal(S(""), a)
  end

  def test_class_new
    assert_equal("RUBY", S("RUBY"))
  end

  def test_concat
    assert_equal(S("world!"), S("world").concat(33))
    assert_equal(S("world!"), S("world").concat(S('!')))
  end

  def test_count
    a = S("hello world")
    assert_equal(5, a.count(S("lo")))
    assert_equal(0, a.count(S("lo"), S("h")))
    assert_equal(2, a.count(S("lo"), S("o")))
    assert_equal(8, a.count(S("^l")))
    assert_equal(4, a.count(S("hello"), S("^l")))
    assert_equal(4, a.count(S("ej-m")))
    assert_equal(2, a.count(S("aeiou"), S("^e")))
  end

  def test_crypt
    assert_equal(S('aaGUC/JkO9/Sc'), S("mypassword").crypt(S("aa")))
    assert(S('aaGUC/JkO9/Sc') != S("mypassword").crypt(S("ab")))

    # "salt" should be at least 2 characters
    assert_raise(ArgumentError) { S("mypassword").crypt("a")}
  end

  def test_delete
    a = S("hello")
    assert_equal(S("heo"),   a.delete(S("l"), S("lo")))
    assert_equal(S("hello"), a.delete(S("lo"), S("h")))
    assert_equal(S("he"),    a.delete(S("lo")))
    assert_equal(S("hell"),  a.delete(S("aeiou"), S("^e")))
    assert_equal(S("ho"),    a.delete(S("ej-m")))
  end

  def test_delete_bang
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

  def test_downcase_bang
    a = S("helLO")
    assert_equal(S("hello"), a.downcase!)
    assert_equal(S("hello"), a)

    a = S("hello")
    assert_nil(a.downcase!)
    assert_equal(S("hello"), a)
  end

  def test_downcase_bang_multibyte
    # TODO: flunk "No downcase! multibyte tests yet"
  end

  def test_dump
    a= S("Test") << 1 << 2 << 3 << 9 << 13 << 10
    assert_equal(S('"Test\\001\\002\\003\\t\\r\\n"'), a.dump)
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

  def test_empty_eh
    assert(S("").empty?)
    assert(!S("not").empty?)
  end

  def test_eql_eh
    a = S("hello")
    assert a.eql?(S("hello"))
    assert a.eql?(a)
  end

  def test_equals2
    assert_equal(false, S("foo") == :foo)
    assert_equal(false, S("foo") == :foo)

    assert(S("abcdef") == S("abcdef"))

    assert(S("CAT") != S('cat'))
    assert(S("CaT") != S('cAt'))
  end

  def test_equals3
    assert_equal(false, S("foo") === :foo)
    casetest(S("abcdef"), S("abcdef"))
    casetest(S("CAT"), S('cat'), true) # Reverse the test - we don't want to
    casetest(S("CaT"), S('cAt'), true) # find these in the case.
  end

  def test_equalstilde
    # str =~ str
    assert_raises TypeError do
      assert_equal 10,  S("FeeFieFoo-Fum") =~ S("Fum")
    end

    # "str =~ regexp" same as "regexp =~ str"
    assert_equal 10,  S("FeeFieFoo-Fum") =~ /Fum$/
    assert_equal nil, S("FeeFieFoo-Fum") =~ /FUM$/

    # "str =~ obj" calls  "obj =~ str"
    assert_equal ["aaa",  123],  "aaa" =~ MatchDefiner.new(123)
    assert_equal ["bbb", :foo],  "bbb" =~ MatchDefiner.new(:foo)
    assert_equal ["ccc",  nil],  "ccc" =~ MatchDefiner.new(nil)

    # default Object#=~ method.
    assert_equal false,  "a string" =~ Object.new
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

  def test_gsub_bang
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
    assert_not_equal(S("hello").hash, S("helLO").hash)
  end

  def test_hex
    assert_equal(0,    S("0").hex, "0")
    assert_equal(0,    S("0x0").hex, "0x0")
    assert_equal(255,  S("0xff").hex, "0xff")
    assert_equal(-255, S("-0xff").hex, "-0xff")
    assert_equal(255,  S("0xFF").hex, "0xFF")
    assert_equal(-255, S("-0xFF").hex, "-0xFF")
    assert_equal(255,  S("0Xff").hex, "0Xff")
    assert_equal(255,  S("ff").hex, "ff")
    assert_equal(-255, S("-ff").hex, "-ff")
    assert_equal(255,  S("FF").hex, "FF")
    assert_equal(-255, S("-FF").hex, "-FF")
    assert_equal(0,    S("-ralph").hex, '-ralph')
    assert_equal(-15,  S("-fred").hex, '-fred')
    assert_equal(15,   S("fred").hex, 'fred')
    assert_equal(-15,  S("-Fred").hex, '-Fred')
    assert_equal(15,   S("Fred").hex, 'Fred')
  end

  def test_include_eh
    assert_equal true,  S("foobar").include?(S("foo"))
    assert_equal false, S("foobar").include?(S("baz"))

    assert_equal true,  S("foobar").include?(?f)
    assert_equal false, S("foobar").include?(?z)

    assert_raises TypeError do
      S('').include? :junk
    end
  end

  def test_index
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
    assert_equal("",       S("FooBar")[6..2])
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

    # TODO: figure out why there were two test_index's and how to consolidate

    assert_equal 0, S("hello").index(?h)
    assert_equal 3, S("hello").index(?l, 3)

    assert_nil      S("hello").index(?z)
    assert_nil      S("hello").index(?z, 3)

    assert_equal 1, S("hello").index(S("ell"))
    assert_equal 3, S("hello").index(S("l"), 3)

    assert_nil      S("hello").index(/z./)
    assert_nil      S("hello").index(S("z"), 3)

    assert_equal 2, S("hello").index(/ll./)
    assert_equal 3, S("hello").index(/l./, 3)

    assert_nil      S("hello").index(S("z"))
    assert_nil      S("hello").index(/z./, 3)

#    flunk "No backref tests" # HACK uncomment
  end

  def test_index_equals
    s = S("FooBar")
    s[0] = S('A')
    assert_equal(S("AooBar"), s)

    s[-1]= S('B')
    assert_equal(S("AooBaB"), s)
    assert_raises(IndexError) { s[-7] = S("xyz") }
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

  def test_insert
    assert_equal S("BCAD"), S("AD").insert(0, S("BC"))
    assert_equal S("ABCD"), S("AD").insert(1, S("BC"))
    assert_equal S("ADBC"), S("AD").insert(2, S("BC"))

    assert_raises(IndexError) { S("AD").insert(3, S("BC")) }

    assert_equal S("ADBC"), S("AD").insert(-1, S("BC"))
    assert_equal S("ABCD"), S("AD").insert(-2, S("BC"))
    assert_equal S("BCAD"), S("AD").insert(-3, S("BC"))

    assert_raises(IndexError) { S("AD").insert(-4, S("BC")) }

    s = S("AD")
    s.insert 0, S("BC")
    assert_equal S("BCAD"), s
  end

  # Need to make sure that we get back exactly what we want, intern is safe
  # for this.  (test/unit calls inspect on results, which is useless for
  # debugging this.)

  def test_inspect
    assert_equal :'""',       S("").inspect.intern
    assert_equal :'"string"', S("string").inspect.intern
    assert_equal :'"\""',     S("\"").inspect.intern
    assert_equal :'"\\\\"',   S("\\").inspect.intern
    assert_equal :'"\n"',     S("\n").inspect.intern
    assert_equal :'"\r"',     S("\r").inspect.intern
    assert_equal :'"\t"',     S("\t").inspect.intern
    assert_equal :'"\f"',     S("\f").inspect.intern
    assert_equal :'"\001"',   S("\001").inspect.intern
    assert_equal :'"\b"',   S("\010").inspect.intern
    assert_equal :'"\177"',   S("\177").inspect.intern
    assert_equal :'"\377"',   S("\377").inspect.intern

    assert_equal :'"\\#{1}"', (S("#") + S("{1}")).inspect.intern

    assert_equal :'"\\#$f"',  (S("#") + S("$f")).inspect.intern
    assert_equal :'"\\#@f"',  (S("#") + S("@f")).inspect.intern
  end

  def test_intern
    assert_equal(:koala, S("koala").intern)
    assert(:koala !=     S("Koala").intern)

    # error cases
    assert_raise(ArgumentError) { S("").intern }
    assert_raise(ArgumentError) { S("with\0null\0inside").intern }
  end

  def test_length
    assert_equal(0, S("").length)
    assert_equal(4, S("1234").length)
    assert_equal(6, S("1234\r\n").length)
    assert_equal(7, S("\0011234\r\n").length)
  end

  def test_ljust
    assert_equal S(""),     S("").ljust(-1)
    assert_equal S(""),     S("").ljust(0)
    assert_equal S(" "),    S("").ljust(1)
    assert_equal S("  "),   S("").ljust(2)
    
    assert_equal S("@"),    S("@").ljust(0)
    assert_equal S("@"),    S("@").ljust(1)
    assert_equal S("@ "),   S("@").ljust(2)
    assert_equal S("@  "),  S("@").ljust(3)

    assert_equal S("@@"),   S("@@").ljust(1)
    assert_equal S("@@"),   S("@@").ljust(2)
    assert_equal S("@@ "),  S("@@").ljust(3)
    assert_equal S("@@  "), S("@@").ljust(4)

    assert_equal(S("@X"),   S("@").ljust(2, "X"))
    assert_equal(S("@XX"),  S("@").ljust(3, "X"))

    assert_equal(S("@X"),   S("@").ljust(2, "XY"))
    assert_equal(S("@XY"),  S("@").ljust(3, "XY"))
    assert_equal(S("@XY"),  S("@").ljust(3, "XY"))
    assert_equal(S("@XYX"), S("@").ljust(4, "XY"))

    assert_equal S("@@"),   S("@@").ljust(1, "XY")
    assert_equal S("@@"),   S("@@").ljust(2, "XY")
    assert_equal S("@@X"),  S("@@").ljust(3, "XY")
    assert_equal S("@@XY"), S("@@").ljust(4, "XY")

    # zero width padding
    assert_raises ArgumentError do
      S("hi").ljust(0, "")
    end
  end

  def test_lstrip
    a = S("  hello")
    assert_equal(S("hello"), a.lstrip)
    assert_equal(S("  hello"), a)
    assert_equal(S("hello "), S(" hello ").lstrip)
    assert_equal(S("hello"), S("hello").lstrip)
  end

  def test_lstrip_bang
    a = S("  abc")
    b = a.dup
    assert_equal(S("abc"), a.lstrip!)
    assert_equal(S("abc"), a)
    assert_equal(S("  abc"), b)
  end

  def test_lt2
    assert_equal(S("world!"), S("world") << 33)
    assert_equal(S("world!"), S("world") << S('!'))
  end

  def test_match
    a = S("cruel world")

    m = a.match(/\w+/)
    assert_kind_of MatchData, m
    assert_equal S("cruel"), m.to_s

    m = a.match '\w+'
    assert_kind_of MatchData, m
    assert_equal S("cruel"), m.to_s

    assert_raises TypeError do
      a.match Object.new
    end

    o = Object.new
    def o.to_str() return '\w+' end
    m = a.match o
    assert_kind_of MatchData, m
    assert_equal S("cruel"), m.to_s
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

  def test_next_bang
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
    assert_equal(0,    S("0").oct, "0")
    assert_equal(255,  S("0377").oct, "0377")
    assert_equal(-255, S("-0377").oct, "-0377")
    assert_equal(255,  S("377").oct, "377")
    assert_equal(-255, S("-377").oct, "-377")
    assert_equal(24,   S("030X").oct, "030X")
    assert_equal(-24,  S("-030X").oct, "-030X")
    assert_equal(0,    S("ralph").oct, "ralph")
    assert_equal(0,    S("-ralph").oct, "-ralph")
  end

  def test_percent
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

  def test_plus
    s1 = S('')
    s2 = S('')
    s3 = s1 + s2
    assert_equal S(''), s3
    assert_not_equal s1.object_id, s3.object_id
    assert_not_equal s2.object_id, s3.object_id

    s1 = S('yo')
    s2 = S('')
    s3 = s1 + s2
    assert_equal S('yo'), s3
    assert_not_equal s1.object_id, s3.object_id

    s1 = S('')
    s2 = S('yo')
    s3 = s1 + s2
    assert_equal S('yo'), s3
    assert_not_equal s2.object_id, s3.object_id

    s1 = S('yo')
    s2 = S('del')
    s3 = s1 + s2
    assert_equal S('yodel'), s3
    assert_equal false, s3.tainted?

    s1 = S('yo')
    s2 = S('del')
    s1.taint
    s3 = s1 + s2
    assert_equal true, s3.tainted?

    s1 = S('yo')
    s2 = S('del')
    s2.taint
    s3 = s1 + s2
    assert_equal true, s3.tainted?

    s1 = S('yo')
    s2 = S('del')
    s1.taint
    s2.taint
    s3 = s1 + s2
    assert_equal true, s3.tainted?
  end

  def test_replace
    a = S("foo")
    assert_equal S("f"), a.replace(S("f"))

    a = S("foo")
    assert_equal S("foobar"), a.replace(S("foobar"))

    a = S("foo")
    a.taint
    b = a.replace S("xyz")
    assert_equal S("xyz"), b

    assert b.tainted?, "Replaced string should be tainted"
  end

  def test_reverse
    a = S("beta")
    assert_equal(S("ateb"), a.reverse)
    assert_equal(S("beta"), a)
  end

  def test_reverse_bang
    a = S("beta")
    assert_equal(S("ateb"), a.reverse!)
    assert_equal(S("ateb"), a)
  end

  def test_rindex
    # String
    assert_equal 0, S('').rindex(S(''))
    assert_equal 3, S('foo').rindex(S(''))
    assert_nil   S('').rindex(S('x'))

    assert_equal 6, S("ell, hello").rindex(S("ell"))
    assert_equal 3, S("hello,lo").rindex(S("l"), 3)

    assert_nil   S("hello").rindex(S("z"))
    assert_nil   S("hello").rindex(S("z"), 3)

    # Fixnum
    assert_nil S('').rindex(0)

    assert_equal 3, S("hello").rindex(?l)
    assert_equal 3, S("hello,lo").rindex(?l, 3)

    assert_nil S("hello").rindex(?z)
    assert_nil S("hello").rindex(?z, 3)

    assert_nil S('').rindex(256)

    assert_nil S('').rindex(-1)

    # Regexp
    assert_equal 0, S('').rindex(//)
    assert_equal 5, S('hello').rindex(//)

    assert_nil S('').rindex(/x/)

    assert_equal 7, S("ell, hello").rindex(/ll./)
    assert_equal 3, S("hello,lo").rindex(/l./, 3)

    assert_nil S("hello").rindex(/z./,   3)
    assert_nil S("hello").rindex(/z./)
  end

  def test_rjust
    assert_equal S(""),     S("").rjust(-1)
    assert_equal S(""),     S("").rjust(0)
    assert_equal S(" "),    S("").rjust(1)
    assert_equal S("  "),   S("").rjust(2)
    
    assert_equal S("@"),    S("@").rjust(0)
    assert_equal S("@"),    S("@").rjust(1)
    assert_equal S(" @"),   S("@").rjust(2)
    assert_equal S("  @"),  S("@").rjust(3)

    assert_equal S("@@"),   S("@@").rjust(1)
    assert_equal S("@@"),   S("@@").rjust(2)
    assert_equal S(" @@"),  S("@@").rjust(3)
    assert_equal S("  @@"), S("@@").rjust(4)

    assert_equal(S("X@"),   S("@").rjust(2, "X"))
    assert_equal(S("XX@"),  S("@").rjust(3, "X"))

    assert_equal(S("X@"),   S("@").rjust(2, "XY"))
    assert_equal(S("XY@"),  S("@").rjust(3, "XY"))
    assert_equal(S("XY@"),  S("@").rjust(3, "XY"))
    assert_equal(S("XYX@"), S("@").rjust(4, "XY"))

    assert_equal S("@@"),   S("@@").rjust(1, "XY")
    assert_equal S("@@"),   S("@@").rjust(2, "XY")
    assert_equal S("X@@"),  S("@@").rjust(3, "XY")
    assert_equal S("XY@@"), S("@@").rjust(4, "XY")

    # zero width padding
    assert_raises ArgumentError do
      S("hi").rjust(0, "")
    end
  end

  def test_rstrip
    a = S("hello  ")
    assert_equal(S("hello"), a.rstrip)
    assert_equal(S("hello  "), a)
    assert_equal(S(" hello"), S(" hello ").rstrip)
    assert_equal(S("hello"), S("hello").rstrip)
  end

  def test_rstrip_bang
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
#    Version.less_than("1.8.2") do
#      assert_nil(S("FooBar").slice(6..2))
#    end
#    Version.greater_or_equal("1.8.2") do
      assert_equal("", S("FooBar").slice(6..2))
#    end
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

  def test_slice_bang
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
      assert_raises(:IndexError) { a.slice!(6) }
    end 
    assert_equal(S("FooBar"), a)

    if @aref_slicebang_silent
      assert_nil( a.slice!(-7) ) 
    else 
      assert_raises(:IndexError) { a.slice!(-7) }
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
      assert_raises(:IndexError) {a.slice!(7,2)}     # Maybe should be six?
    end
    assert_equal(S("FooBar"), a)
    if @aref_slicebang_silent
      assert_nil(a.slice!(-7,10))
    else
      assert_raises(:IndexError) {a.slice!(-7,10)}
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
#      Version.less_than("1.8.2") do
#        assert_nil(a.slice!(6..2))
#      end
#      Version.greater_or_equal("1.8.2") do
        assert_equal("", a.slice!(6..2))
#      end
    else
      assert_raises(:RangeError) {a.slice!(6..2)}
    end
    assert_equal(S("FooBar"), a)
    if @aref_slicebang_silent
      assert_nil(a.slice!(-10..-7))
    else
      assert_raises(:RangeError) {a.slice!(-10..-7)}
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
      assert_raises(:IndexError) {a.slice!(/xyzzy/)}
    end
    assert_equal(S("FooBar"), a)
    if @aref_slicebang_silent
      assert_nil(a.slice!(/plugh/))
    else
      assert_raises(:IndexError) {a.slice!(/plugh/)}
    end
    assert_equal(S("FooBar"), a)

    a=S("FooBar")
    assert_equal(S("Foo"), a.slice!(S("Foo")))
    assert_equal(S("Bar"), a)

    a=S("FooBar")
    assert_equal(S("Bar"), a.slice!(S("Bar")))
    assert_equal(S("Foo"), a)
  end

  def test_spaceship
    assert_equal( 1, S("abcdef") <=> S("ABCDEF"))
    assert_equal(-1, S("ABCDEF") <=> S("abcdef"))

    assert_equal( 1, S("abcdef") <=> S("abcde") )
    assert_equal( 0, S("abcdef") <=> S("abcdef"))
    assert_equal(-1, S("abcde")  <=> S("abcdef"))
  end

  def test_split
    original_dollar_semi = $;.nil? ? $; : $;.dup

    assert_equal [], S("").split
    assert_equal [], S("").split(nil)
    assert_equal [], S("").split(' ')
    assert_equal [], S("").split(nil, 1)
    assert_equal [], S("").split(' ', 1)

    str = S("a")
    arr = str.split(nil, 1)
    assert_equal ["a"], arr
    assert_equal str.object_id, arr.first.object_id

    # Tests of #split's behavior with a pattern of ' ' or $; == nil
    $; = nil
    assert_equal [S("a"), S("b")], S("a b")       .split
    assert_equal [S("a"), S("b")], S("a  b")      .split
    assert_equal [S("a"), S("b")], S(" a b")      .split
    assert_equal [S("a"), S("b")], S(" a b ")     .split
    assert_equal [S("a"), S("b")], S("  a b ")    .split
    assert_equal [S("a"), S("b")], S("  a  b ")   .split
    assert_equal [S("a"), S("b")], S("  a b  ")   .split
    assert_equal [S("a"), S("b")], S("  a  b  ")  .split

    assert_equal [S("a"), S("b")], S("a\tb")      .split
    assert_equal [S("a"), S("b")], S("a\t\tb")    .split
    assert_equal [S("a"), S("b")], S("a\nb")      .split
    assert_equal [S("a"), S("b")], S("a\n\nb")    .split
    assert_equal [S("a"), S("b")], S("a\rb")      .split
    assert_equal [S("a"), S("b")], S("a\r\rb")    .split

    assert_equal [S("a"), S("b")], S("a\t b")     .split
    assert_equal [S("a"), S("b")], S("a\t b")     .split
    assert_equal [S("a"), S("b")], S("a\t\nb")    .split
    assert_equal [S("a"), S("b")], S("a\r\nb")    .split
    assert_equal [S("a"), S("b")], S("a\r\n\r\nb").split

    assert_equal [S("a"), S("b"), S("c")], S(" a   b\t c ").split

    assert_equal [S("a"), S("b")], S("a b")       .split(S(" "))
    assert_equal [S("a"), S("b")], S("a  b")      .split(S(" "))
    assert_equal [S("a"), S("b")], S(" a b")      .split(S(" "))
    assert_equal [S("a"), S("b")], S(" a b ")     .split(S(" "))
    assert_equal [S("a"), S("b")], S("  a b ")    .split(S(" "))
    assert_equal [S("a"), S("b")], S("  a  b ")   .split(S(" "))
    assert_equal [S("a"), S("b")], S("  a b  ")   .split(S(" "))
    assert_equal [S("a"), S("b")], S("  a  b  ")  .split(S(" "))

    assert_equal [S("a"), S("b")], S("a\tb")      .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\nb")      .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\rb")      .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\vb")      .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\t\tb")    .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\n\nb")    .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\r\rb")    .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\v\vb")    .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\v\vb")    .split(S(" "))

    assert_equal [S("a"), S("b")], S("a\t b")     .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\t b")     .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\t\nb")    .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\r\nb")    .split(S(" "))
    assert_equal [S("a"), S("b")], S("a\r\n\r\nb").split(S(" "))

    assert_equal [S("a"), S("b"), S("")], S("  a  b  ").split(S(" "), -2)
    assert_equal [S("a"), S("b"), S("")], S("  a  b  ").split(S(" "), -1)
    assert_equal [S("a"), S("b")],        S("  a  b  ").split(S(" "),  0)
    assert_equal [S("  a  b  ")],         S("  a  b  ").split(S(" "),  1)
    assert_equal [S("a"), S("b  ")],      S("  a  b  ").split(S(" "),  2)
    assert_equal [S("a"), S("b"), S("")], S("  a  b  ").split(S(" "),  3)
    assert_equal [S("a"), S("b"), S("")], S("  a  b  ").split(S(" "),  4)

    assert_equal [S("a"), S("b"), S("c")], S(" a   b\t c ").split(S(" "))

    assert_equal [S("a"), S("b")], S("a b").split(nil)

    # These tests are all for various patterns.

    assert_equal [S("a b")],                S("a b")     .split(S("  "))
    assert_equal [S("a"), S("b")],          S("a  b")    .split(S("  "))
    assert_equal [S(" a b")],               S(" a b")    .split(S("  "))
    assert_equal [S(" a b ")],              S(" a b ")   .split(S("  "))
    assert_equal [S(""),  S("a b ")],       S("  a b ")  .split(S("  "))
    assert_equal [S(""),  S("a"), S("b ")], S("  a  b ") .split(S("  "))
    assert_equal [S(""),  S("a b")],        S("  a b  ") .split(S("  "))
    assert_equal [S(""),  S("a"), S("b")],  S("  a  b  ").split(S("  "))

    $; = '|'
    assert_equal [S("a"), S("b")],                          S("a|b")     .split
    assert_equal [S("a"), S(""),  S("b")],                  S("a||b")    .split
    assert_equal [S(""),  S("a"), S("b")],                  S("|a|b")    .split
    assert_equal [S(""),  S("a"), S("b")],                  S("|a|b|")   .split
    assert_equal [S(""),  S(""),  S("a"),  S("b")],         S("||a|b|")  .split
    assert_equal [S(""),  S(""),  S("a"),  S(""),  S("b")], S("||a||b|") .split
    assert_equal [S(""),  S(""),  S("a"),  S("b")],         S("||a|b||") .split
    assert_equal [S(""),  S(""),  S("a"),  S(""),  S("b")], S("||a||b||").split

    assert_equal [S("a"), S("b"), S("c")],        S("a|b|c") .split(S("|"))
    assert_equal [S("a"), S(""), S("c")],         S("a||c")  .split(S("|"))
    assert_equal [S(""), S("a"), S("b"), S("c")], S("|a|b|c").split(S("|"))

    assert_equal [S("a b")], S("a b").split(nil)
    assert_equal [S("a"), S("b")], S("a|b").split(nil)

    # Regexp

    assert_equal [S("a"), S("b"), S("c")], S("abc").split(//)
    assert_equal [S("a"), S("b"), S("c")], S("abc").split(//i)

    assert_equal [S("a"), S("b"), S("c")], S("a|b|c").split(S('|'), -1)
    assert_equal [S("a"), S("b"), S("c")], S("a|b|c").split(S('|'),  0)
    assert_equal [S("a|b|c")],             S("a|b|c").split(S('|'),  1)
    assert_equal [S("a"), S("b|c")],       S("a|b|c").split(S('|'),  2)
    assert_equal [S("a"), S("b"), S("c")], S("a|b|c").split(S('|'),  3)
    assert_equal [S("a"), S("b"), S("c")], S("a|b|c").split(S('|'),  4)

    assert_equal [S("a"), S("b")],        S("a|b|").split(S('|'))
    assert_equal([S("a"), S("b"), S("")], S("a|b|").split(S('|'), -1))

    assert_equal [S("a"), S(""), S("b"), S("c")], S("a||b|c|").split(S('|'))
    assert_equal([S("a"), S(""), S("b"), S("c"), S("")],
                 S("a||b|c|").split(S('|'), -1))

    assert_equal [S("a"), S("b")],                 S("a b")   .split(/ /)
    assert_equal [S("a"), S(""),  S("b")],         S("a  b")  .split(/ /)
    assert_equal [S(""),  S("a"), S("b")],         S(" a b")  .split(/ /)
    assert_equal [S(""),  S("a"), S("b")],         S(" a b ") .split(/ /)
    assert_equal [S(""),  S(""),  S("a"), S("b")], S("  a b ").split(/ /)
    assert_equal([S(""),  S(""),  S("a"), S(""), S("b")],
                 S("  a  b ").split(/ /))
    assert_equal([S(""),  S(""),  S("a"), S("b")],
                 S("  a b  ").split(/ /))
    assert_equal([S(""),  S(""),  S("a"), S(""), S("b")],
                 S("  a  b  ").split(/ /))

    assert_equal [S("a b")],                S("a b")     .split(/  /)
    assert_equal [S("a"), S("b")],          S("a  b")    .split(/  /)
    assert_equal [S(" a b")],               S(" a b")    .split(/  /)
    assert_equal [S(" a b ")],              S(" a b ")   .split(/  /)
    assert_equal [S(""),  S("a b ")],       S("  a b ")  .split(/  /)
    assert_equal [S(""),  S("a"), S("b ")], S("  a  b ") .split(/  /)
    assert_equal [S(""),  S("a b")],        S("  a b  ") .split(/  /)
    assert_equal [S(""),  S("a"), S("b")],  S("  a  b  ").split(/  /)

    assert_equal([S("a"), S("b")],
                 S("a@b")     .split(/@/))
    assert_equal([S("a"), S(""),  S("b")],
                 S("a@@b")    .split(/@/))
    assert_equal([S(""),  S("a"), S("b")],
                 S("@a@b")    .split(/@/))
    assert_equal([S(""),  S("a"), S("b")],
                 S("@a@b@")   .split(/@/))
    assert_equal([S(""),  S(""),  S("a"),  S("b")],
                 S("@@a@b@")  .split(/@/))
    assert_equal([S(""),  S(""),  S("a"),  S(""),  S("b")],
                 S("@@a@@b@") .split(/@/))
    assert_equal([S(""),  S(""),  S("a"),  S("b")],
                 S("@@a@b@@") .split(/@/))
    assert_equal([S(""),  S(""),  S("a"),  S(""),  S("b")],
                 S("@@a@@b@@").split(/@/))

    assert_equal [S("a"), S("b"), S("c")],        S("a@b@c") .split(/@/)
    assert_equal [S("a"), S(""), S("c")],         S("a@@c")  .split(/@/)
    assert_equal [S(""), S("a"), S("b"), S("c")], S("@a@b@c").split(/@/)

    # grouping

    assert_equal([S('ab'), S('1'), S('cd'), S('2'), S('ef')],
                 S('ab1cd2ef').split(/(\d)/))

    assert_equal([S('ab'), S('1'), S('cd'), S('2'), S('ef')],
                 S('ab1cd2ef').split(/(\d)/, -2))
    assert_equal([S('ab'), S('1'), S('cd'), S('2'), S('ef')],
                 S('ab1cd2ef').split(/(\d)/, -1))
    assert_equal([S('ab'), S('1'), S('cd'), S('2'), S('ef')],
                 S('ab1cd2ef').split(/(\d)/,  0))
    assert_equal([S('ab1cd2ef')],
                 S('ab1cd2ef').split(/(\d)/,  1))
    assert_equal([S('ab'), S('1'), S('cd2ef')],
                 S('ab1cd2ef').split(/(\d)/,  2))
    assert_equal([S('ab'), S('1'), S('cd'), S('2'), S('ef')],
                 S('ab1cd2ef').split(/(\d)/,  3))
    assert_equal([S('ab'), S('1'), S('cd'), S('2'), S('ef')],
                 S('ab1cd2ef').split(/(\d)/,  4))
    assert_equal([S('ab'), S('1'),  S('cd'), S('2'), S('ef')],
                 S('ab1cd2ef').split(/(\d)/,  5))

    # mulitple grouping

    assert_equal([S('ab'), S('1'), S('2'), S('cd'), S('3'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/))

    assert_equal([S('ab'), S('1'), S('2'), S('cd'), S('3'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/, -2))
    assert_equal([S('ab'), S('1'), S('2'), S('cd'), S('3'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/, -1))
    assert_equal([S('ab'), S('1'), S('2'), S('cd'), S('3'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/,  0))
    assert_equal([S('ab12cd34ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/,  1))
    assert_equal([S('ab'), S('1'), S('2'), S('cd34ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/,  2))
    assert_equal([S('ab'), S('1'), S('2'), S('cd'), S('3'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/,  3))
    assert_equal([S('ab'), S('1'), S('2'), S('cd'), S('3'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/,  4))
    assert_equal([S('ab'), S('1'), S('2'), S('cd'), S('3'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d)(\d)/,  5))

    # nested grouping (puke)

    assert_equal([S('ab'), S('12'), S('2'), S('cd'), S('34'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d(\d))/))

    assert_equal([S('ab'), S('12'), S('2'), S('cd'), S('34'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d(\d))/, -2))
    assert_equal([S('ab'), S('12'), S('2'), S('cd'), S('34'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d(\d))/, -1))
    assert_equal([S('ab'), S('12'), S('2'), S('cd'), S('34'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d(\d))/,  0))
    assert_equal([S('ab12cd34ef')],
                 S('ab12cd34ef').split(/(\d(\d))/,  1))
    assert_equal([S('ab'), S('12'), S('2'), S('cd34ef')],
                 S('ab12cd34ef').split(/(\d(\d))/,  2))
    assert_equal([S('ab'), S('12'), S('2'), S('cd'), S('34'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d(\d))/,  3))
    assert_equal([S('ab'), S('12'), S('2'), S('cd'), S('34'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d(\d))/,  4))
    assert_equal([S('ab'), S('12'), S('2'), S('cd'), S('34'), S('4'), S('ef')],
                 S('ab12cd34ef').split(/(\d(\d))/,  5))

    # split any zero-length match

    assert_equal [S('a'), S('b'), S('c')], S('abc').split(/[z]|/)

  ensure
    $; = original_dollar_semi
  end

  def test_split_icky1
    # HACK soo bizarre and ugly.
    assert_equal([S('a'), S('b'), S('c'), S('z')],
                 S('abcz').split(/([z])|/),
                 "S('abcz').split(/([z])|/)")
  end

  def test_split_icky2
    assert_equal([S('c'), S('z'), S('a')],
                 S('cza').split(/([z]|)/),
                 "S('cz').split(/([z]|)/)")
    assert_equal([S('a'), S(''), S('b'), S(''), S('c'), S('z')],
                 S('abcz').split(/([z]|)/),
                 "S('abcz').split(/([z]|)/)")
  end

  def test_squeeze
    assert_equal(S("abc"), S("aaabbbbccc").squeeze)
    assert_equal(S("aa bb cc"), S("aa   bb      cc").squeeze(S(" ")))
    assert_equal(S("BxTyWz"), S("BxxxTyyyWzzzzz").squeeze(S("a-z")))
  end

  def test_squeeze_bang
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

  def test_strip_bang
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
    assert_equal(S("104 ello"), S("hello").sub(/./) { |s| s[0].to_s + S(' ')})
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

  def test_sub_bang
    a = S("hello")
    assert_nil a.sub!(/X/, S('Y'))

    a = S("hello")
    assert_nil a.sub!(/X/) { |s| assert_nil s }

    a = S("hello")
    a.sub!(/[aeiou]/, S('*'))
    assert_equal(S("h*llo"), a)

    a = S("hello")
    a.sub!(/([aeiou])/, S('<\1>'))
    assert_equal(S("h<e>llo"), a)

    a = S("hello")

    a.sub!(/./) do |s|
      assert_equal S('h'), s
      s[0].to_s + S(' ')
    end

    assert_equal(S("104 ello"), a)

    a = S("hello")
    a.sub!(/(hell)(.)/) do |s|
      assert_equal S('hell'), $1
      assert_equal S('o'), $2
      $1.upcase + S('-') + $2
    end
    assert_equal(S("HELL-o"), a)

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

    assert_equal(S("\001\000"), S("\377").succ)
  end

  def test_succ_bang
    a = S("abc")
    assert_equal(S("abd"), a.succ!)
    assert_equal(S("abd"), a)

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

    a = S("\377")
    assert_equal(S("\001\000"), a.succ!)
    assert_equal(S("\001\000"), a)
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

  def test_swapcase_bang
    a = S("hi&LOW")
    b = a.dup
    assert_equal(S("HI&low"), a.swapcase!)
    assert_equal(S("HI&low"), a)
    assert_equal(S("hi&LOW"), b)

    a = S("$^#^%$#!!")
    assert_nil(a.swapcase!)
    assert_equal(S("$^#^%$#!!"), a)
  end

  def test_swapcase_bang_multibyte
    # TODO: flunk "No multibyte tests yet"
  end

  def test_times
    assert_equal(S(""), S("HO") * 0)
    assert_equal(S("HOHO"), S("HO") * 2)
  end

  def test_times_negative
    assert_raises ArgumentError do
      S("str") * -1
    end
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
    assert_equal(a.object_id, a.to_s.object_id)

    b = TestStringSubclass.new("me")
    assert_equal("me", b.to_s)
    assert_not_equal(b.object_id, b.to_s.object_id)
  end

  def test_to_str
    a = S("me")
    assert_equal("me", a.to_str)
    assert_equal(a.object_id, a.to_str.object_id)

    b = TestStringSubclass.new("me")
    assert_equal("me", b.to_str)
    assert_not_equal(b.object_id, b.to_str.object_id)
  end

  def test_to_sym
    assert_equal(:alpha, S("alpha").to_sym)
    assert_equal(:alp_ha, S("alp_ha").to_sym)
    assert_equal(:"9alpha", S("9alpha").to_sym)
    assert_equal(:"9al pha", S("9al pha").to_sym)
  end

  def test_tr
    assert_equal S("hippo"),    S("hello").tr(S("el"), S("ip"))
    assert_equal S("*e**o"),    S("hello").tr(S("^aeiou"), S("*"))
    assert_equal S("hal"),      S("ibm").tr(S("b-z"), S("a-z"))

    assert_equal S("www"), S("abc").tr(S("a-c"), S("w"))
    assert_equal S("wxx"), S("abc").tr(S("a-c"), S("w-x"))
    assert_equal S("wxy"), S("abc").tr(S("a-c"), S("w-y"))
    assert_equal S("wxy"), S("abc").tr(S("a-c"), S("w-z"))

    assert_equal S("wbc"), S("abc").tr(S("a"),   S("w-x"))
    assert_equal S("wxc"), S("abc").tr(S("a-b"), S("w-y"))
  end

  def test_tr_bang
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

  def test_tr_s_bang
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

  def test_upcase_bang
    a = S("hello")
    b = a.dup
    assert_equal(S("HELLO"), a.upcase!)
    assert_equal(S("HELLO"), a)
    assert_equal(S("hello"), b)

    a = S("HELLO")
    assert_nil(a.upcase!)
    assert_equal(S("HELLO"), a)
  end

  def test_upcase_bang_multibyte
    # TODO: flunk "No multibyte tests yet"
  end

  def test_upto
    a     = S("aa")
    start = S("aa")
    count = 0
    val = a.upto S("zz") do |s|
      assert_equal(start, s)
      start.succ!
      count += 1
    end

    assert_equal(S("aa"), val)
    assert_equal(676, count)
  end
end

require 'test/unit' if $0 == __FILE__
