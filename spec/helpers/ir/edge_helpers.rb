require 'java'

import 'org.jruby.ir.util.Edge'

class EdgeType

  def initialize(method, type)
    @method = method
    @edge_type   = type
  end

  def type
    @actual.__send__(@method)
  end

  def matches?(actual)
    @actual = actual
    type == @edge_type
  end
end

module HaveType
  def have_type(type)
    EdgeType.new(:getType, type)
  end
end

class Object
  include HaveType
end
