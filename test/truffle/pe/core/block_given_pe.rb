# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module BlockGivenFixtures

  def self.foo
    block_given?
  end

end

# TODO CS 8-Nov-15 produces a graph that I think should reduce to a constant but doesn't

tagged_example "BlockGivenFixtures.foo", false
tagged_example "BlockGivenFixtures.foo { }", true
