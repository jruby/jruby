require 'java'
require 'jruby.rb'

class String
  def write(*args)
    args.each do |item|
      self << item
    end
    self
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
      # return false if (item.java_class < JRuby::ExpandArrayNode)
    end
    #end
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
      backup = $>
      $> = "new IRubyObject[] {"
      iter = @node.iterator()
      0.upto(@node.size() - 1) do
	item = iter.next()
	$expression.acceptNode(item)
	$> << ", "
      end
      $> << "}"
      result = $>
      $> = backup
      return result
    end
  end
end
