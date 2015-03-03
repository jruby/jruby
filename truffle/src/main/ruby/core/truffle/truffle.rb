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

  # What is the version of Truffle/Graal being used?
  # @return [String] a version string such as `"0.6"`, or `"undefined"` if running on a non-Graal JVM.
  def self.version
    Primitive.graal_version
  end

  # Are we currently running on a Graal VM? If this returns false you are
  # running on a conventional JVM and performance will be much lower.
  def self.graal?
    Primitive.graal?
  end

  # Is this VM built on the SubstrateVM?
  def self.substrate?
    Primitive.substrate?
  end

end
