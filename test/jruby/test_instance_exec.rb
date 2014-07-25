require 'test/unit'

class TestInstanceExec < Test::Unit::TestCase
  # JRUBY-3594
  def test_instance_exec
    o = Object.new
    lam = lambda {|a,b,c,d,e| [a,b,c,d,e]}
    result = o.instance_exec(1,2,3,4,5, &lam)
    assert_equal([1,2,3,4,5], result)
    result = o.instance_exec(1,2,3,4,5) {|a,b,c,d,e| [a,b,c,d,e]}
    assert_equal([1,2,3,4,5], result)
  end
end

