# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle

  # Information about the Truffle runtime system and utilities for interacting
  # with it.
  module Runtime

    # Return the name of the Java class used to represent this object, as a
    # String.
    # @return [String]
    def self.java_class_of(object)
      Truffle::Primitive.java_class_of(object)
    end

    # Dump a string as a String of escaped literal bytes.
    # @return [String]
    def self.dump_string(string)
      Truffle::Primitive.dump_string(string)
    end

    # Print a String directly to stderr without going through the normal
    # Ruby and runtime IO systems.
    # @return [nil]
    def self.debug_print(string)
      Truffle::Primitive.debug_print string
    end

  end

end
