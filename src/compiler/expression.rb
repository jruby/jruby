require 'java'
require 'jruby.rb'
require 'helper.rb'

$expression = JRuby::NodeVisitor.new()

class << $expression
  def method_missing(name, *node)
    puts ("expression #{node} cannot be visited because `#{name}' is not defined")
  end

  def acceptNode(node)
    node.accept(self) unless node.nil?
  end

  def visitStrNode(node)
    print ("RubyString.newString(runtime, \"#{node.getValue()}\")")
  end
end
