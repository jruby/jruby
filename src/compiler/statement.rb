require 'java'
require 'jruby.rb'
require 'helper.rb'
require 'expression.rb'
require 'simple_expression.rb'

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

  def visitIfNode(node)
    if simple_exp?(node.getCondition())
      $> << "if ("
      $expression.acceptNode(node.getCondition())
      $> << ") {\n"
      acceptNode(node.getThenBody())
      $> << "\n} else {\n"
      acceptNode(node.getElseBody())
      $> << "\n}\n"
    else
      nil
      # FIXME
    end
  end
end

#  def visitClassNode(node)
#    puts("if (runtime.getRubyClass().isNil()) {")
#    puts("throw new TypeError(runtime, \"no outer class/module\");")
#    puts("}")
#
#    unless (node.getSuperNode().nil?)
#      $method.local << "RubyClass superClass = null"
#      $method << "try {\n"
#      if (simple_exp?(node.getSuperNode()))
#	$method << "superClass = ";
#	$expression.acceptNode(node.getSuperNode())
#	$method << ";\n"
#      else
#	# FIXME
#      end
#      $method << "} catch (Expression exp) {\n"
#      case node.getSuperNode().type()
#	when JRuby::Colon2Node, JRuby::ConstNode then
#	  $method << "throw new TypeError(runtime, \"undefined superclass '"
#	  $method << node.getSuperNode()).getName()
#	  $method << "'\");\n"
#	else
#	  $method << "throw new TypeError(runtime, \"undefined superclass\");\n"
#      end
#      $method << "if (superClass != null && superClass instanceof MetaClass) {\n"
#      $method << "throw new TypeError(runtime, \"can't make subclass of virtual class\");\n"
#      $method << "}\n"
#    end
#  end

