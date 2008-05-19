require 'minijava'

import "java.util.ArrayList"
import "java.lang.Boolean"

class ArrayList
  include Enumerable
  alias_method :add, :"add(java.lang.Object)"

  def each
    iter = iterator
    while iter.hasNext == Boolean::TRUE
      yield iter.next
    end
  end

  def <<(arg)
    add arg
  end

  class << self
    alias_method :new, :"new()"
  end
end

list = ArrayList.new
obj = Object.new

list.add obj
puts list
puts list.size

list.add(1)
list.add("foo")
list.add(ArrayList.new)

list.each {|x| puts x}
p list.collect {|x| x.to_java rescue x }
