# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius

  class Tuple < Array

    def copy_from(other, start, length, dest)
      # TODO CS 6-Feb-15 use higher level indexing when it works
      length.times do |n|
        self[dest + n] = other[start + n]
      end
    end

  end

end
