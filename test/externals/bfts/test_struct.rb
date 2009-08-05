require 'test/unit'
require 'rubicon_testcase'

class TestStruct < RubiconTestCase

  # TODO: why is this a class variable, and why in setup?
  def setup
    @@struct ||= Struct.new 'TestStruct', :alpha, :bravo
  end

  def test_clone
    for taint in [ false, true ]
      for frozen in [ false, true ]
        a = @@struct.new
        a.alpha = 112
        a.taint  if taint
        a.freeze if frozen
        b = a.clone

        assert_equal(a, b)
        assert(a.__id__ != b.__id__)
        assert_equal(a.frozen?,  b.frozen?)
        assert_equal(a.tainted?, b.tainted?)
        assert_equal(a.alpha,    b.alpha)
      end
    end
  end

  def test_each_pair
    # TODO: raise NotImplementedError, 'Need to write test_each_pair'
  end

  # No longer a valid test in 1.8.7
=begin
  def test_each
    assert_raises LocalJumpError do
      @@struct.new.each
    end

    a = []
    @@struct.new('a', 'b').each { |x| a << x }
    assert_equal ['a', 'b'], a
  end
=end

  def test_eql_eh
    # TODO: raise NotImplementedError, 'Need to write test_eql_eh'
  end

  def test_equals2
    ts1 = @@struct.new 64, 42
    ts2 = @@struct.new 64, 42

    assert_equal ts1, ts2

    ts3 = @@struct.new 64

    assert_not_equal ts1, ts3

    os1 = Struct.new('OtherStruct',  :alpha, :bravo).new 64, 42

    assert_not_equal os1, ts1

    os2 = Struct.new('OtherStruct2', :alpha, :bravo, :charlie).new 64, 42

    assert_not_equal os2, ts1
  end

  def test_hash
    # TODO: raise NotImplementedError, 'Need to write test_hash'
  end

  def test_index_equals
    t = @@struct.new
    assert_nothing_raised do
      t[:alpha] = 64
      assert_equal t.alpha, 64

      t['bravo'] = 112
      assert_equal t.bravo, 112

      t[0] = 65
      assert_equal t.alpha, 65

      t[1] = 113
      assert_equal t.bravo, 113

      t[-2] = 66
      assert_equal t.alpha, 66
    end

    assert_raises NameError do
      t['gamma'] = 1
    end
    assert_raise IndexError do
      t[2] = 1
    end
  end

  def test_index
    t = @@struct.new 64, 112

    assert_equal 64,  t['alpha']
    assert_equal 64,  t[:alpha]

    assert_equal 64,  t[0]
    assert_equal 112, t[1]
    assert_equal 112, t[-1]

    assert_equal 112, t[1.5]
  
    assert_raises NameError do
      t['gamma']
    end

    assert_raises IndexError do
      t[2]
    end
  end

  def test_initialize
    assert_instance_of Class, @@struct
    assert_equal Struct::TestStruct, @@struct
    assert_instance_of Struct::TestStruct, @@struct.new(5)

    t1 = @@struct.new
    assert_equal nil, t1.alpha
    assert_equal nil, t1.bravo

    t2 = @@struct.new 1
    assert_equal 1,   t2.alpha
    assert_equal nil, t2.bravo

    t3 = @@struct.new 2, 3
    assert_equal 2, t3.alpha
    assert_equal 3, t3.bravo

    assert_raises ArgumentError do
      @@struct.new 4, 5, 6
    end
  end

  def test_to_s
    expected = "#<struct Struct::TestStruct alpha=\"a\", bravo=\"b\">"
    assert_equal expected, @@struct.new('a', 'b').to_s
  end

  def test_inspect
    s = @@struct.new('a', 'b')
    expected = "#<struct Struct::TestStruct alpha=\"a\", bravo=\"b\">"
    assert_equal expected, s.inspect
  end

  def test_length
    t = @@struct.new
    assert_equal(2,t.length)
  end

  def test_members
    assert_equal ["alpha", "bravo"], @@struct.members
  end

  def test_members
    assert_equal ["alpha", "bravo"], @@struct.new.members
  end

  def test_select
    struct = @@struct.new 'a', 'b'
    assert_equal ['a'], struct.select { |item| item == 'a' }
  end

  # REFACTOR / length
  def test_size
    t = @@struct.new
    assert_equal(2, t.size)
  end

  def test_to_a
    t = @@struct.new 'a', 'b'
    assert_equal ['a', 'b'], t.to_a
  end

  def test_values_at
    struct = @@struct.new 'a', 'b'
    assert_equal ['b'], struct.values_at(-1)
    assert_equal ['a'], struct.values_at(0)
    assert_equal ['b'], struct.values_at(1)
    assert_equal ['a', 'b'], struct.values_at(0..1)
  end

  def test_values
    t = @@struct.new 'a', 'b'
    assert_equal ['a', 'b'], t.values
  end

end

