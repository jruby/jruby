# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module SuperFixtures
  class Parent
    def call_super(n)
      n * 2
    end

    def call_zsuper(n)
      n * 3
    end
  end

  class Child < Parent
    def call_super(n)
      super(n)
    end

    def call_zsuper(n)
      super
    end
  end

  INSTANCE = Child.new
end

example "SuperFixtures::INSTANCE.call_super(42)", 84
example "SuperFixtures::INSTANCE.call_zsuper(24)", 72

example "SuperFixtures::Child.new.call_super(42)", 84
example "SuperFixtures::Child.new.call_zsuper(24)", 72
