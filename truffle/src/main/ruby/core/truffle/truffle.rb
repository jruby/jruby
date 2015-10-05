# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Methods specific to the Truffle implementation. This module is only available
# when running with `-X+T+`. `defined? Truffle` can therefore be used as a
# runtime test for the program running in Truffle mode.
module Truffle

  # The version of Truffle and Graal in use.
  # @return [String] a version string such as `"0.7"`, or `"undefined"` if running on a non-Graal JVM.
  def self.version
    Primitive.graal_version
  end

  # Tests if the program is using the Graal VM.
  def self.graal?
    Primitive.graal?
  end

  # Tests if this VM is a SubstrateVM build.
  def self.substrate?
    Primitive.substrate?
  end

  # Return the Binding of the method which calls this method.
  # @return [Binding]
  # @!scope class
  define_singleton_method :binding_of_caller, Truffle::Primitive.method(:binding_of_caller).unbind

  # Returns the source (such as the file name) of the method which calls this
  # method.
  # @return [String]
  # @!scope class
  define_singleton_method :source_of_caller, Truffle::Primitive.method(:source_of_caller).unbind

end
