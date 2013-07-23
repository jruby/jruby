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

class Vertex
  def add_edge(options=nil)
    self.addEdgeTo(options[:to], options[:type])
  end

  def remove_edge(options=nil)
    self.removeEdgeTo(options[:to])
  end

  def remove_edges(options={})
    case options[:direction]
    when :in
      self.removeAllIncomingEdges()
    when :out
      self.removeAllOutgoingEdges()
    else
      self.removeAllEdges()
    end
  end

  def outgoing_edge(options=nil)
    if options.nil?
      self.getOutgoingEdge
    else
      self.getOutgoingEdgeOfType(options[:type])
    end
  end

  def incoming_edge(options=nil)
    if options.nil?
      self.getIncomingEdge
    else
      self.getIncomingEdgeOfType(options[:type])
    end
  end

  def data(options={})
    case options[:direction]
    when :in
      if options.keys.include?(:type)
        self.getIncomingSourceDataOfType(options[:type])
      else
        self.getIncomingSourceData()
      end
    when :out
      if options.keys.include?(:type)
        self.getOutgoingDestinationDataOfType(options[:type])
      else
        self.getOutgoingDestinationData()
      end
    end
  end
end
