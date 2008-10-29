require 'test/unit'

class TestEval < Test::Unit::TestCase
  class ToStr; def to_str; ''; end; end
  class ToInt; def to_int; 1; end; end

  def test_args_coersion
    assert_raises(TypeError) { eval :foo }
    assert_raises(TypeError) { eval 1 }
    assert_nothing_raised { eval "" }
    assert_nothing_raised { eval ToStr.new }
    assert_raises(TypeError) { eval "", :foo }
    assert_raises(TypeError) { eval "", 1 }
    assert_nothing_raised { eval "", binding }
    assert_nothing_raised { eval "", proc{} }
    assert_raises(TypeError) { eval "", binding, :foo }
    assert_raises(TypeError) { eval "", binding, 1 }
    assert_nothing_raised { eval "", binding, 'foo' }
    assert_nothing_raised { eval "", binding, ToStr.new }
    assert_raises(TypeError) { eval "", binding, 'foo', 'foo' }
    assert_nothing_raised { eval "", binding, 'foo', 1 }
    assert_nothing_raised { eval "", binding, 'foo', :foo }
    assert_nothing_raised { eval "", binding, 'foo', ToInt.new }
  end

  module Mod
    def m1; @result << "M1"; end
  end
  
  CHANGE='
  alias_method :m1_orig, :m1
  
  def m1
   m1_orig
   @result << "M2"
  end
  '
  
  class Test
    include Mod  
    eval CHANGE

    def initialize; @result = []; end
    def t1; m1; end
  end

  def test_eval_alias_method
    o = Test.new
    assert_equal(["M1", "M2"], o.t1)
  end
end
