require 'test/unit'
require 'java'
require 'jruby'

class TestRewriter < Test::Unit::TestCase
  RW = org.jruby.ast.visitor.rewriter.ReWriteVisitor

  def test_roundtrip
    method = <<-EOS
      def foo(a, b = 2, *c)
        x = a + b + c.length
        x.times { def bar; end }
        return x
      end
    EOS

    node = JRuby.parse(method, 'something.rb')
    
    assert_nothing_raised { RW.create_code_from_node(node, '') }
  end
end
