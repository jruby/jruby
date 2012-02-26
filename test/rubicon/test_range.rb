require 'test/unit'

class TestRange < Test::Unit::TestCase
  IS19 = RUBY_VERSION =~ /1\.9/

  # a simple class with a 'succ' method
  #
  class SuccDefiner

    include Comparable

    attr_reader :n

    def initialize(n)
      @n = n
    end

    def succ
      SuccDefiner.new(@n + 1)
    end

    def <=>(other)
      @n <=> other.n
    end

  end

  ############################################################
  # Test Utilities:

  #
  # Tests where the methods == and eql? behave the same.
  # This is true when the class of the endpoints of
  # the Range have == and eql? methods that are
  # aliases. This in turn is true for most classes,
  # for example for the class used here: Fixnum.
  #
  # TODO: write tests using an endpoint class where
  # the methods  == and eql? differ.
  #
  def util_test_equals(method)

    r25 = Range.new(2, 5)
    r34 = Range.new(3, 4)
    r35 = Range.new(3, 5)
    r36 = Range.new(3, 6)
    r45 = Range.new(4, 5)
    rx25 = Range.new(2, 5, true)
    rx34 = Range.new(3, 4, true)
    rx35 = Range.new(3, 5, true)
    rx36 = Range.new(3, 6, true)
    rx45 = Range.new(4, 5, true)

    # closed interval
    assert_equal(false, r35.send(method, r34))
    assert_equal(true,  r35.send(method, r35))
    assert_equal(false, r35.send(method, r36))

    assert_equal(false, r35.send(method, r25))
    assert_equal(false, r35.send(method, r45))

    # half-open interval
    assert_equal(false, rx35.send(method, rx34))
    assert_equal(true,  rx35.send(method, rx35))
    assert_equal(false, rx35.send(method, rx36))

    assert_equal(false, rx35.send(method, rx25))
    assert_equal(false, rx35.send(method, rx45))

    # half-open / closed interval: never equal
    assert_equal(false, rx35.send(method, r34))
    assert_equal(false, rx35.send(method, r35))
    assert_equal(false, rx35.send(method, r36))

    assert_equal(false, rx35.send(method, r25))
    assert_equal(false, rx35.send(method, r45))

    # closed / half-open interval: never equal
    assert_equal(false, r35.send(method, rx34))
    assert_equal(false, r35.send(method, rx35))
    assert_equal(false, r35.send(method, rx36))

    assert_equal(false, r35.send(method, rx25))
    assert_equal(false, r35.send(method, rx45))

    # non-Range argument
    assert_equal(false, r35.send(method, Object.new))
    assert_equal(false, rx35.send(method, Object.new))

  end

  def util_test_end(msg)
    assert_equal(10, Range.new(1, 10).send(msg))
    assert_equal(11, Range.new(1, 11, true).send(msg))
    assert_equal("z", Range.new("a", "z").send(msg))
    assert_equal("A", Range.new("a", "A", true).send(msg))
  end

  def util_member(method)
    is = [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]
    xs = is.map {|x| 0.5 + x}
    iss = ["aj", "ak", "al", "am", "an" ,"ao" "ap"]
    xss = ["ajjj", "akkk", "alll", "ammm", "annn" ,"aooo" "appp"]

    r47  = Range.new(4, 7)
    rx47 = Range.new(4, 7, true)
    r_akam  = Range.new("ak", "am")
    rx_akam = Range.new("ak", "am", true)

    # as discrete range
    assert_equal([4, 5, 6, 7 ], is.select {|i| r47.send(method, i) })
    assert_equal([4, 5, 6],     is.select {|i| rx47.send(method, i) })

    assert_equal(["ak","al","am"], iss.select {|i| r_akam.send(method, i) })
    assert_equal(["ak","al"],      iss.select {|i| rx_akam.send(method, i) })

    # as continuous range
    assert_equal([4.5, 5.5, 6.5], xs.select {|x| r47.send(method, x) })
    assert_equal([4.5, 5.5, 6.5], xs.select {|x| rx47.send(method, x) })

    unless IS19
      assert_equal(["akkk","alll"], xss.select {|i| r_akam.send(method, i) })
      assert_equal(["akkk","alll"], xss.select {|i| rx_akam.send(method, i) })
    end
    
    # non-comparable argument
    assert_equal(false, Range.new(5, 10)  === Object.new)
    assert_equal(false, Range.new(5, 10, true) === Object.new)
  end

  def util_step_tester(acc_facit, range, *step_args)
    acc = []
    res = range.step(*step_args) {|x| acc << x }
    assert_equal(acc_facit, acc)
    assert_same(range, res)
  end

  def util_each(first, last, exclude, expected)
    index = first
    count = 0
    Range.new(first, last, exclude).each do |x|
      assert_equal(index, x)
      index = index.succ
      count += 1
    end
    assert_equal(expected, count)
  end

  ############################################################
  # Test Methods

  # TODO: need to test new/initialize, esp w/ bad values

  def test_begin
    assert_equal(1,   Range.new(1, 10).begin)
    assert_equal("a", Range.new("a", "z").begin)
    assert_equal(1,   Range.new(1, 10, true).begin)
    assert_equal("a", Range.new("a", "z", true).begin)
  end

  def test_each
    util_each(1, 10, false, 10)
    util_each(1, 10,  true,  9)

    util_each("A", "J", false, 10)
    util_each("A", "J",  true,  9)

    # test something that has a .succ, but is neither int nor string
    t1 = SuccDefiner.new(1)
    t10 = SuccDefiner.new(10)
    util_each(t1, t10, false, 10)
    util_each(t1, t10,  true,  9)

    # test something that has no .succ:
# HACK:
#    util_each(Object.new, Object.new, false, 10)
#    util_each(Object.new, Object.new,  true,  9)
  end

  def test_eql_eh
    util_test_equals(:eql?)
  end

  def test_equals2 # '=='
    util_test_equals(:==)
  end

  def test_equals3 # '==='
    util_member(:===)

    # misc
    gotit = false
    case 52
      when Range.new(0, 49)
        fail("Shouldn't have matched")
      when Range.new(50, 75)
        gotit = true
      else
        fail("Shouldn't have matched")
    end
    assert_equal(true,gotit)

    gotit = false
    case 50
      when Range.new(0, 49)
        fail("Shouldn't have matched")
      when Range.new(50, 75)
        gotit = true
      else
        fail("Shouldn't have matched")
    end
    assert_equal(true,gotit)

    gotit = false
    case 75
      when Range.new(0, 49)
        fail("Shouldn't have matched")
      when Range.new(50, 75)
        gotit = true
      else
        fail("Shouldn't have matched")
    end
    assert_equal(true,gotit)
  end

  def test_exclude_end_eh
    assert_equal(true, Range.new(1, 10, true).exclude_end?)
    assert_equal(false,Range.new(1, 10).exclude_end?)
    assert_equal(true, Range.new("A", "Z", true).exclude_end?)
    assert_equal(false,Range.new("A", "Z").exclude_end?)
  end

  def test_first
    assert_equal(1, Range.new(1, 10).first)
    assert_equal("a", Range.new("a", "z").first)
    assert_equal(1, Range.new(1, 10, true).first)
    assert_equal("a", Range.new("a", "z", true).first)
  end

  def test_hash
    assert_equal(Range.new(5, 9).hash, Range.new(5, 9).hash)
    assert_equal(Range.new("A", "Z").hash, Range.new("A", "Z").hash)

    assert_equal(Range.new(5, 9, true).hash, Range.new(5, 9, true).hash)
    assert_equal(Range.new("A", "Z", true).hash, Range.new("A", "Z", true).hash)

    assert_not_equal(Range.new(5, 9).hash, Range.new(5, 9, true).hash)
    assert_not_equal(Range.new("A", "Z").hash, Range.new("A", "Z", true).hash)

    assert_not_equal(Range.new(5, 9).hash, Range.new(5, 8).hash)
    assert_not_equal(Range.new("A", "Z").hash, Range.new("a", "Z", true).hash)
  end

  def test_include_eh
    util_member(:include?)
  end

  def test_end
    util_test_end(:end)
  end

  def test_last
    util_test_end(:last)
  end

  def test_member_eh
    util_member(:member?)
  end

  def test_step
    # n=1 default in step(n)
    util_step_tester([5,6,7,8,9], Range.new(5, 9))
    util_step_tester([5,6,7,8],   Range.new(5, 9, true))

    # explicit n=1
    util_step_tester([5,6,7,8,9], Range.new(5, 9),   1)
    util_step_tester([5,6,7,8],   Range.new(5, 9, true),  1)

    # n=2
    util_step_tester([5,7,9],     Range.new(5, 9),   2)
    util_step_tester([5,7],       Range.new(5, 9, true),  2)

    # n=3
    util_step_tester([5,8],       Range.new(5, 9),   3)
    util_step_tester([5,8],       Range.new(5, 9, true),  3)

    # n=4
    util_step_tester([5,9],       Range.new(5, 9),   4)
    util_step_tester([5],         Range.new(5, 9, true),  4)
  end

  def test_to_s
    assert_equal('1..10', Range.new(1, 10).to_s)
    assert_equal('1...10', Range.new(1, 10, true).to_s)
    assert_equal('a..z', Range.new('a', 'z').to_s)
    assert_equal('a...z', Range.new('a', 'z', true).to_s)
  end

  def test_inspect
    assert_equal('1..10', Range.new(1, 10).inspect)
    assert_equal('1...10', Range.new(1, 10, true).inspect)
    assert_equal('"a".."z"', Range.new('a', 'z').inspect)
    assert_equal('"a"..."z"', Range.new('a', 'z', true).inspect)
  end
end