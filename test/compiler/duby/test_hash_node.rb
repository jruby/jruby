require 'test/unit'
require 'jruby'
require 'compiler/duby'

class TestHashNode < Test::Unit::TestCase
  def test_signature
    node = JRuby.parse("class Foo; def foo(a); {a => java.lang.String, :return => java.lang.System}; end; end")
    hash_node = node.child_nodes[0].next_node.body_node.child_nodes[0].body_node.child_nodes[0]
    
    builder = Compiler::FileBuilder.new("foo.rb")
    node.compile(builder)
    assert_equal([java.lang.System, java.lang.String], hash_node.signature(nil))
  end
end
