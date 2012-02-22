require 'test/unit'
require 'java'

class TestInstantiatingInterfaces < Test::Unit::TestCase
  class A
    include java.lang.Runnable
  end

  def test_instantiating_interfaces
    assert_raises(NoMethodError) do
      A.new.run
    end
    foo = nil
    ev = java.lang.Runnable.impl do
      foo = "ran"
    end
    ev.run rescue nil
    assert_equal("ran", foo)

    cs = java.lang.CharSequence.impl(:charAt) do |sym, index|
      assert_equal(:charAt, sym)
      assert_equal(0, index)
    end

    assert_nothing_raised { cs.charAt(0) }
    assert_raises(NoMethodError) do
      cs.length
    end
  end
end
