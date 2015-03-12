# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module PushingPixelsFixtures

  module Foo
    extend self

    def foo(a, b, c)
      hash = {a: a, b: b, c: c}
      array = hash.map { |k, v| v }
      x = array[0]
      y = [a, b, c].sort[1]
      x + y
    end

  end

  class Bar

    def method_missing(method, *args)
      if Foo.respond_to?(method)
        Foo.send(method, *args)
      else
        0
      end
    end

  end

end

tagged_example "PushingPixelsFixtures::Bar.new.foo(14, 8, 6)"
