class Binding
  # :nodoc:
  def irb
    require 'irb'
    irb
  end
end

module Kernel
  def pp(*objs)
    require 'pp'
    pp(*objs)
  end

  private :pp
end

autoload :Set, 'set'

module Enumerable
  # Makes a set from the enumerable object with given arguments.
  def to_set(klass = Set, *args, &block)
    klass.new(self, *args, &block)
  end
end
