require 'java'
require 'jruby'
require 'test/unit'

class TestASTInspector < Test::Unit::TestCase
  import org.jruby.compiler.ASTInspector

  def test_multi_when_optimizes_correctly
    # JRUBY-3479: ASTInspector was not walking else node correctly in multi-when cases
    code = "case 1; when 2, 3; else; begin; 1; rescue; end; end"
    node = JRuby.parse(code)
    inspector = ASTInspector.new
    inspector.inspect(node)
    
    assert inspector.has_frame_aware_methods
    assert inspector.has_scope_aware_methods
  end
end
