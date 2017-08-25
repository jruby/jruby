require 'test/unit'

class TestJRubyCoreExt < Test::Unit::TestCase

  def setup
    require 'jruby'; require 'jruby/core_ext'
  end

  def test_jrubyc_inspect_bytecode
    compiled = JRuby.compile script = 'def foo; return :foo end; bar = foo()'
    assert_equal script, compiled.to_s
    assert bytecode = compiled.inspect_bytecode
    assert_equal String, bytecode.class
  end

  def test_subclasses
    superclass = Class.new
    sub1 = Class.new(superclass)
    sub2 = Class.new(superclass)
    assert_equal([sub1.to_s, sub2.to_s].sort, superclass.subclasses.map{|c| c.to_s}.sort)
  end

  def test_with_current_runtime_as_global
    other_runtime = org.jruby.Ruby.newInstance
    other_runtime.use_as_global_runtime
    assert_equal other_runtime, org.jruby.Ruby.global_runtime
    JRuby.with_current_runtime_as_global do
      assert_equal JRuby.runtime, org.jruby.Ruby.global_runtime
    end
    assert_equal other_runtime, org.jruby.Ruby.global_runtime
  end
end
