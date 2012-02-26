require 'test/unit'


class TestMarshal < Test::Unit::TestCase
  IS19 = RUBY_VERSION =~ /1\.9/

  #
  # Check that two arrays contain the same "bag" of elements.
  # A mathematical bag differs from a "set" by counting the
  # occurences of each element. So as a bag [1,2,1] differs from
  # [2,1] (but is equal to [1,1,2]).
  #
  # The method only relies on the == operator to match objects
  # from the two arrays. The elements of the arrays may contain
  # objects that are not "Comparable".
  #
  # FIXME: This should be moved to common location.
  def assert_bag_equal(expected, actual)
    # For each object in "actual" we remove an equal object
    # from "expected". If we can match objects pairwise from the
    # two arrays we have two equal "bags". The method Array#index
    # uses == internally. We operate on a copy of "expected" to
    # avoid destructively changing the argument.
    #
    expected_left = expected.dup
    actual.each do |x|
      if j = expected_left.index(x)
        expected_left.slice!(j)
      end
    end
    assert( expected.length == actual.length && expected_left.length == 0,
           "Expected: #{expected.inspect}, Actual: #{actual.inspect}")
  end

  class A
    attr :a1
    attr :a2
    def initialize(a1, a2)
      @a1, @a2 = a1, a2
    end
  end

  class B
    attr :b1
    attr :b2
    def initialize(b1, b2)
      @b1 = A.new(b1, 2*b1)
      @b2 = b2
    end
  end

  # Dump/load to string
  def test_s_dump_load1
    b = B.new(10, "wombat")
    assert_equal(10,       b.b1.a1)
    assert_equal(20,       b.b1.a2)
    assert_equal("wombat", b.b2)

    s = Marshal.dump(b)

    assert_instance_of(String, s)

    newb = Marshal.load(s)
    assert_equal(10,       newb.b1.a1)
    assert_equal(20,       newb.b1.a2)
    assert_equal("wombat", newb.b2)

    assert(newb.__id__ != b.__id__)

    assert_raise(ArgumentError) { Marshal.dump(b, 1) }
  end

  def test_s_dump_load2
    b = B.new(10, "wombat")
    assert_equal(10,       b.b1.a1)
    assert_equal(20,       b.b1.a2)
    assert_equal("wombat", b.b2)

    File.open("_dl", "w") { |f| Marshal.dump(b, f) }
    
    begin
      newb = nil
      File.open("_dl") { |f| newb = Marshal.load(f) }

      assert_equal(10,       newb.b1.a1)
      assert_equal(20,       newb.b1.a2)
      assert_equal("wombat", newb.b2)
      
    ensure
      File.delete("_dl")
    end
  end

  unless IS19
    def test_s_dump_load3
      b = B.new(10, "wombat")
      s = Marshal.dump(b)

      res = []
      newb = Marshal.load(s, proc { |obj| res << obj unless obj.kind_of?(Fixnum)})

      assert_equal(10,       newb.b1.a1)
      assert_equal(20,       newb.b1.a2)
      assert_equal("wombat", newb.b2)

      assert_bag_equal([newb, newb.b1, newb.b2], res)
    end
  end

  # there was a bug Marshaling Bignums, so

  def test_s_dump_load4
    b1 = 123456789012345678901234567890
    b2 = -123**99
    b3 = 2**32
    assert_equal(b1, Marshal.load(Marshal.dump(b1)))
    assert_equal(b2, Marshal.load(Marshal.dump(b2)))
    assert_equal(b3, Marshal.load(Marshal.dump(b3)))
  end

  def test_s_dump_load5
    x = [1, 2, 3, [4, 5, "foo"], {1=>"bar"}, 2.5, 9**30]
    y = Marshal.dump(x)
    assert_equal(x, Marshal.load(y))
  end

  def test_s_restore
    b = B.new(10, "wombat")
    assert_equal(10,       b.b1.a1)
    assert_equal(20,       b.b1.a2)
    assert_equal("wombat", b.b2)

    s = Marshal.dump(b)

    assert_instance_of(String, s)

    newb = Marshal.restore(s)
    assert_equal(10,       newb.b1.a1)
    assert_equal(20,       newb.b1.a2)
    assert_equal("wombat", newb.b2)

    assert(newb.__id__ != b.__id__)
  end

end