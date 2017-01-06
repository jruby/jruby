# Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle::Debug

  class Breakpoint

    def initialize(handle)
      @handle = handle
    end

    def remove
      Truffle::Debug.remove_handle @handle
    end

  end

  def self.break(file, line, &block)
    Breakpoint.new(break_handle(File.expand_path(file), line, &block))
  end

end
