require 'test/unit'

class TestStruct < Test::Unit::TestCase
  IS19 = RUBY_VERSION =~ /1\.9/

  def setup
    if !defined? Struct::StructForTesting
      @@myclass = Struct.new("StructForTesting", :alpha, :bravo)
    end
  end

  def test_00s_new
    assert_equal(Struct::StructForTesting, @@myclass)
    t1 = @@myclass.new
    t1.alpha = 1
    t1.bravo = 2
    assert_equal(1,t1.alpha)
    assert_equal(2,t1.bravo)
    t2 = Struct::StructForTesting.new
    assert_equal(t1.class, t2.class)
    t2.alpha = 3
    t2.bravo = 4
    assert_equal(3,t2.alpha)
    assert_equal(4,t2.bravo)
    assert_raise(ArgumentError) { Struct::StructForTesting.new(1,2,3) }

    t3 = @@myclass.new(5,6)
    assert_equal(5,t3.alpha)
    assert_equal(6,t3.bravo)
  end

  def test_AREF # '[]'
    t = Struct::StructForTesting.new
    t.alpha = 64
    t.bravo = 112
    assert_equal(64,  t["alpha"])
    assert_equal(64,  t[0])
    assert_equal(112, t[1])
    assert_equal(112, t[-1])
    assert_equal(t["alpha"], t[:alpha])
  
    assert_raise(NameError)  { p t["gamma"] }
    assert_raise(IndexError) { p t[2] }
  end

  def test_ASET # '[]='
    t = Struct::StructForTesting.new
    t.alpha = 64
    assert_equal(64,t["alpha"])
    assert_equal(t["alpha"],t[:alpha])
    assert_raise(NameError) { t["gamma"]=1 }
    assert_raise(IndexError) { t[2]=1 }
  end

  def test_EQUAL # '=='
    t1 = Struct::StructForTesting.new
    t1.alpha = 64
    t1.bravo = 42
    t2 = Struct::StructForTesting.new
    t2.alpha = 64
    t2.bravo = 42
    assert_equal(t1,t2)
  end

  def test_clone
    for taint in [ false, true ]
      for frozen in [ false, true ]
        a = Struct::StructForTesting.new
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

  def test_each
    a=[]
    Struct::StructForTesting.new('a', 'b').each {|x|
      a << x
    }
    assert_equal(['a','b'], a)
  end

  def test_length
    t = Struct::StructForTesting.new
    assert_equal(2,t.length)
  end

  def test_members
    assert_equal(Struct::StructForTesting.members, IS19 ? [:alpha, :bravo] : [ "alpha", "bravo" ])
  end

  def test_size
    t = Struct::StructForTesting.new
    assert_equal(2, t.length)
  end

  def test_to_a
    t = Struct::StructForTesting.new('a','b')
    assert_equal(['a','b'], t.to_a)
  end

  def test_values
    t = Struct::StructForTesting.new('a','b')
    assert_equal(['a','b'], t.values)
  end

  def test_anonymous_struct
    t = Struct.new(:foo)
    u = Struct.new(nil, :foo)

    if IS19
      assert_equal(nil, t.name)
      assert_equal(nil, u.name)
    else
      assert_equal("", t.name)
      assert_equal("", u.name)
    end
    assert_equal('foo', t.new('foo')[:foo])
    assert_equal('foo', u.new('foo')[:foo])
    assert_equal('foo', t.new('foo').foo)
    assert_equal('foo', u.new('foo').foo)
  end


end
