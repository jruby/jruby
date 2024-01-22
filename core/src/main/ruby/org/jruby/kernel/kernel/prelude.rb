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
