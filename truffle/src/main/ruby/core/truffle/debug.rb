# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle

  module Debug

    def self.break(file = nil, line = nil)
      if line.nil?
        raise 'must specify both a file and a line, or neither' unless file.nil?
        Truffle::Primitive.simple_shell
      else
        Truffle::Primitive.attach file, line do |binding|
          Truffle::Primitive.simple_shell
        end
      end
    end

  end

end
