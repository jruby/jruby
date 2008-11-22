require 'test/unit'

class TestSymbol19 < Test::Unit::TestCase
  class SymTest
    attr_accessor :call_parameters
    def call(*args)
      self.call_parameters = args
    end
  end

  def test_to_proc
    assert Symbol.instance_methods.include?(:to_proc)
  
    s = SymTest.new
    [s].each &:call
    assert_equal [], s.call_parameters

    assert_equal 3, (:+).to_proc.call(1,2)
  end
end
