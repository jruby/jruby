# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Kernel

  def printf(*args)
    print sprintf(*args)
  end
  module_function :printf

  alias_method :trust, :untaint
  alias_method :untrust, :taint
  alias_method :untrusted?, :tainted?

  def caller(start = 1, limit = nil)
    start += 1
    if limit.nil?
      args = [start]
    else
      args = [start, limit]
    end
    caller_locations(*args).map(&:inspect)
  end
  module_function :caller

  def at_exit(&block)
    Truffle::Primitive.at_exit false, &block
  end
  private :at_exit
  module_function :at_exit


end
