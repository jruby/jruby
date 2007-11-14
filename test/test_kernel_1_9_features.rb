require 'test/unit'

class TestKernel19Features < Test::Unit::TestCase
  def test_proc_lambda
    puts 'here'
    # proc does not check arity in 1.9
    assert_nothing_raised { proc {|a,b|}.call(1) }
  end
end