require 'java'
require 'jruby.rb'
require 'helper.rb'

$simple_expression = JRuby::NodeVisitor.new()

def simple_exp? (node)
  $simple_expression.simple?(node)
end

class << $simple_expression
  def method_missing(name, *node)
  end

  def simple?(node)
    @simple = true
    node.accept(self) if node
    @simple
  end

  def visitIfNode(node)
    @simple = simple?(node.getCondition()) and simple?(node.getThenBody()) and simple?(node.getElseBody())
  end
end
