require 'java'

import 'org.jruby.ir.util.Vertex'

class DegreeMatcher

  def initialize(method, degree)
    @method = method
    @value  = degree
  end

  def degree
    @actual.__send__(@method)
  end

  def matches?(actual)
    @actual = actual
    degree == @value
  end

end

module HaveInDegree
  def have_in_degree(degree)
    DegreeMatcher.new(:inDegree, degree)
  end
end

module HaveOutDegree
  def have_out_degree(degree)
    DegreeMatcher.new(:outDegree, degree)
  end
end

class Object
  include HaveInDegree
  include HaveOutDegree
end
