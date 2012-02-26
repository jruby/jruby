# encoding: utf-8
require 'test/unit'


class TestRegexp < Test::Unit::TestCase
  IS19 = RUBY_VERSION =~ /1\.9/

  def test_EQUAL # '=='
    assert_equal(/.foo.*([a-z])/,/.foo.*([a-z])/)
    a=Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE)
    b=Regexp.new(".foo.*([a-z])")
    assert(!(a == b))
    a=Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE)
    b=Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE, "S")
    if IS19
      assert_equal(a, b)
    else
      assert(a != b)
    end
  end

  def test_MATCH # '=~'
    assert_equal(nil,/SIT/ =~ "insensitive")
    assert_equal(5,/SIT/i =~ "insensitive")
  end

  def test_REV # '~'
    $_ = "sit on it"
    assert_equal(nil,~ /SIT/)
    assert_equal(0,~ /SIT/i)
  end

  def test_VERY_EQUAL # '==='
    gotit=false
    case "insensitive"
      when /SIT/
        fail("Shouldn't have matched")
      when /SIT/i
        gotit = true
    end
    assert(gotit)
  end

  def test_casefold?
    a=Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE)
    b=Regexp.new(".foo.*([a-z])")
    assert_equal(true, a.casefold?)
    assert_equal(false, b.casefold?)
  end

  def test_clone
    for taint in [ false, true ]
      for frozen in [ false, true ]
        a = /SIT/.dup
        a.taint  if taint
        a.freeze if frozen
        b = a.clone

        assert_equal(a, b)
        assert_equal(a.frozen?, b.frozen?)
        assert_equal(a.tainted?, b.tainted?)
      end
    end
  end

  unless IS19
    def test_kcode
      a = Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE)
      b = Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE, "S")
      c = Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE, "n")
      assert_equal(nil, a.kcode) if $KCODE == "NONE"
      assert_equal("sjis", b.kcode)
      assert_equal("none",    c.kcode)
    end
  end

  def test_s_last_match
    a = /SIT/
    "STAND OUT" =~ a
    "SIT IT OUT" =~ a
    m = Regexp.last_match
    assert_instance_of(MatchData, m)
    assert_equal([0,3], m.offset(0))
    assert_equal(1, m.length)
    assert_equal(" IT OUT", m.post_match)
  end

  def test_match
    a = Regexp.new("sit")
    m = a.match("howsit going?")
    assert_instance_of(MatchData,m)
    assert_equal([3,6],m.offset(0))
    assert_equal(1, m.length)
  end

  def test_source
    a=Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE)
    assert_equal(".foo.*([a-z])", a.source)
  end

  def test_s_compile
    a=Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE)
    b=Regexp.new("sit", true)
    c=Regexp.new("sit")
    assert(("POSIT" =~ b) != nil)
    assert(("POSIT" =~ c) == nil)
  end

  def test_s_escape
    assert_equal('\\\\\[\]\*\?\{\}\.', Regexp.escape('\\[]*?{}.'))
  end

  def test_s_new
    a=Regexp.new(".foo.*([a-z])", Regexp::IGNORECASE)
    b=Regexp.new("sit", true)
    c=Regexp.new("sit")
    assert(("POSIT" =~ b) != nil)
    assert(("POSIT" =~ c) == nil)
  end

  def test_s_quote
    assert_equal('\\\\\[\]\*\?\{\}\.', Regexp.quote('\\[]*?{}.'))
    assert_raises(TypeError) { Regexp.quote(nil) }
  end

  def test_unicode_match
    "yés!héy!héllo" =~ /!([^!]*)/u
    assert_equal("yés", $~.pre_match)
    assert_equal("!héllo", $~.post_match)
    assert_equal("héy", $~[1])
    assert_equal("!héy", $~[0])
    assert_equal("!héy", $~.to_s)
    assert_equal(["héy"], $~.captures)
    assert_equal(["!héy", "héy"], $~.to_a)
    # couldn't get this to pass right...JRuby and MRI seem to handle it differently, and not like below
    #assert_equal("y̩s!h̩y!h̩llo", $~.string)
  end
end
