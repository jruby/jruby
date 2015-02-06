# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius
  module Mirror
    class Array

      def self.reflect(array)
        Array.new(array)
      end

      def initialize(array)
        @array = array
      end

      def total
        @array.size
      end

      def tuple
        @array
      end

      def start
        0
      end

    end
  end
end
