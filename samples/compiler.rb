require 'samples/parser.rb'

module JRuby
  include_package 'org.jruby.ast.visitor'
end

node = JRuby::Parser::parse "puts()"

statement = JRuby::NodeVisitor.new()
expression = JRuby::NodeVisitor.new()

class << statement
  def method_missing(name, *node)
    puts ("#{node} cannot be visited because `#{name}' is not defined")
  end

  def acceptNode(node)
    node.accept(self) unless node.nil?
  end

  def visitNewlineNode(node)
    acceptNode(node.getNextNode())
  end

  def visitFCallNode(node)
    args = node.getArgsNode()
    init_args(args)
    puts "self.getMetaClass().call(self, \"#{node.getName}\", #{call_args(args)}, CallType.FUNCTIONAL);"
  end

  private
  def init_args(node)
    return if node.nil?
  end

  def call_args(node)
    return "IRubyObject.NULL_ARRAY" if node.nil?
  end
end

puts <<END
import org.jruby.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.*;

public class RubyScript {
  public static void load(Ruby runtime, IRubyObject self) {
    
END

statement.acceptNode(node)

puts <<END
  }

  public static void main(String[] args) {
    Ruby runtime = Ruby.getDefaultInstance();
    load(runtime, runtime.getTopSelf());
  }
}
END
