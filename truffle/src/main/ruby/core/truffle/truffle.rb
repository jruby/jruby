# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
  class << self
    # The version of Truffle and Graal in use.
    # @return [String] a version string such as `"0.7"`, or `"undefined"` if running on a non-Graal JVM.
    def version
      Primitive.graal_version
    end

    # Tests if the program is using the Graal VM.
    def graal?
      Primitive.graal?
    end
    
    def cext?
      Interop.mime_type_supported?('application/x-sulong-library')
    end

    # Tests if this VM is a SubstrateVM build.
    def substrate?
      Primitive.substrate?
    end

    # Return the Binding of the method which calls this method.
    # @return [Binding]
    define_method :binding_of_caller, Primitive.instance_method(:binding_of_caller)

    # Returns the source (such as the file name) of the method which calls this
    # method.
    # @return [String]
    define_method :source_of_caller, Primitive.instance_method(:source_of_caller)
  end
end
