require 'samples/parser.rb'

module JRuby
  include_package 'org.jruby.ast.visitor'
  include_package 'org.jruby.ast'
end

class String
  def write(*args)
    args.each do |item|
      self << item
    end
  end
end

node = JRuby::Parser::parse "puts 'Hello World'"

$statement = JRuby::NodeVisitor.new()
$expression = JRuby::NodeVisitor.new()

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

class CallArgs
  def initialize(node)
    @node = node
  end

  def simple?
    return true if @node.nil?
    # if (@node.java_class < JRuby::ArrayNode)
      iter = @node.iterator()
      0.upto(@node.size() - 1) do
	item = iter.next()
	# return false if (item.java_class() < JRuby::ExpandArrayNode)
      end
      # end
    return true
    # FIXME
  end

  def init
    return "" if simple?
    # FIXME
  end

  def call
    return "IRubyObject.NULL_ARRAY" if @node.nil?
    if (simple?)
      $> = "new IRubyObject[] {"
      iter = @node.iterator()
      0.upto(@node.size() - 1) do
	item = iter.next()
	$expression.acceptNode(item)
	$> << ", "
      end
      $> << "}"
      result = $>
      $> = $stdout
      return result
    end
    # FIXME
  end
end

puts <<END
import org.jruby.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.*;

public class RubyScript {
  public static void load(Ruby runtime, IRubyObject self) {
    
END

$statement.acceptNode(node)

puts <<END
  }

  public static void main(String[] args) {
    Ruby runtime = Ruby.getDefaultInstance();
    load(runtime, runtime.getTopSelf());
  }
}
END
