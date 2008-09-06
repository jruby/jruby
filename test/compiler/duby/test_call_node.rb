require 'test/unit'
require 'jruby'
require 'compiler/duby'

class TestCallNode < Test::Unit::TestCase
  def test_declared_type
    node = JRuby.parse("java.lang.System")
    call_node = node.child_nodes[0].next_node
    assert_equal(java.lang.System, call_node.declared_type(nil))
  end
  
  def test_type
    node = JRuby.parse("a = 'foo'; a.toString").child_nodes[0]
    var_node = node.child_nodes[0].next_node
    call_node = node.child_nodes[1].next_node
    
    builder = Compiler::ClassBuilder.new("foo", "foo")
    builder.method2("foo") do |method|
      var_node.compile(method)

      assert_equal(java.lang.String, call_node.type(method))
    end
  end
end
