require 'java'
require 'jruby.rb'
require 'helper.rb'

$statement = JRuby::NodeVisitor.new()

class << $statement
  def method_missing(name, *node)
    puts ("statement #{node} cannot be visited because `#{name}' is not defined")
  end

  def acceptNode(node)
    node.accept(self) unless node.nil?
  end

  def visitNewlineNode(node)
    acceptNode(node.getNextNode())
  end

  def visitFCallNode(node)
    args = CallArgs.new(node.getArgsNode())
    puts args.init()
    puts "self.getMetaClass().call(self, \"#{node.getName}\", #{args.call()}, CallType.FUNCTIONAL);"
  end
end
