require 'java'
require 'jruby.rb'
require 'helper.rb'

$expression = JRuby::NodeVisitor.new()

class << $expression
  def method_missing(name, *node)
    puts ("expression #{node} cannot be visited because `#{name}' is not defined")
  end

  def acceptNode(node)
    if node.nil?
      $> << "runtime.getNil()"
    else
      node.accept(self)
    end
  end

  def visitStrNode(node)
    $> << "RubyString.newString(runtime, \"#{node.getValue()}\")"
  end

  def visitTrueNode(node)
    $> << "runtime.getTrue()"
  end

  def visitFalseNode(node)
    $> << "runtime.getFalse()"
  end

  def visitSelfNode(node)
    $> << "self"
  end

  def visitIfNode(node)
    if simple_exp?(node)
      acceptNode(node.getCondition())
      $> << " ? "
      acceptNode(node.getThenBody())
      $> << " : "
      acceptNode(node.getElseBody())
    else
      # FIXME
    end
  end
end
