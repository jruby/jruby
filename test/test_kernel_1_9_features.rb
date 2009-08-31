require 'test/unit'

class A
  define_singleton_method(:a) { 42 }
  define_singleton_method(:b, lambda {|x| 2*x })
end

class TestKernel19Features < Test::Unit::TestCase
  def test_proc_lambda
    # proc does not check arity in 1.9
    assert_nothing_raised { proc {|a,b|}.call(1) }
  end

  def test_define_singleton_method
    assert(::A.singleton_methods.include?(:a), "singleton method :a defined")
    assert_equal( 42, A.a, "singleton method :a returns 42" )

    assert(::A.singleton_methods.include?(:b), "singleton method :b defined")
    assert_equal( 84, A.b(42), "singleton method :b returns double the input")
  end
end