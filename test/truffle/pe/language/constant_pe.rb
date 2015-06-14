# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module ConstantFixtures
  A = 1

  def self.get_existing
    Proc
  end

  def self.get
    A
  end

  def self.get_const_get(name)
    ConstantFixtures.const_get(name)
  end

  module Nested
    def self.get_nested
      A
    end
  end

  class Base
    B = 2
  end

  class Child < Base
    def self.get_inherited
      B
    end
  end

  module ConstMissing
    def self.get_missing
      NO_SUCH_NAMED_CONSTANT
    end

    def self.const_missing(const)
      const
    end
  end
end

example "ConstantFixtures.get_existing"
example "ConstantFixtures.get"
example "ConstantFixtures.get_const_get(:A)"

# Internal Graal compiler error
tagged_example "ConstantFixtures.get_const_get('A')"

example "ConstantFixtures::Nested.get_nested"
example "ConstantFixtures::Child.get_inherited"

example "ConstantFixtures::ConstMissing.get_missing"
