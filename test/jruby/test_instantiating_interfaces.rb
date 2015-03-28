require 'test/unit'

class TestInstantiatingInterfaces < Test::Unit::TestCase

  require 'java'

  class NoRun
    include java.lang.Runnable
  end

  class Runner
    include java.lang.Runnable
    def run; 'run' end
  end

  def test_include_mixin
    mixin = NoRun.new
    assert_raises(NoMethodError) { mixin.run }
    Runner.new.run
  end

  def test_impl_proc
    foo = nil
    java.lang.Runnable.impl do
      foo = "ran"
    end.run
    assert_equal("ran", foo)

    cs = java.lang.CharSequence.impl(:charAt) do |sym, index|
      assert_equal(:charAt, sym)
      assert_equal(0, index)
    end
    assert_nothing_raised { cs.charAt(0) }
    assert_raises(NoMethodError) { cs.length }

    calls = []
    cs = java.lang.CharSequence.impl(:charAt, :length) do |sym, *args|
      calls << sym; calls << args
      sym == :length ? 2 : ' '
    end
    assert_equal ' ', cs.charAt(0)
    assert_equal ' ', cs.charAt(1)
    assert_equal 2, cs.length

    assert_equal [ :charAt, [0], :charAt, [1], :length, [] ], calls

    def cs.length; 3 end
    def cs.char_at; nil end

    assert_equal 3, cs.length
    assert_equal ' ', cs.charAt(2)
  end



end
