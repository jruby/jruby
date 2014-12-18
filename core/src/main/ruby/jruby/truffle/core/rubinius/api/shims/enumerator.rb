# Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Enumerator

  def initialize(array)
    @array = array
  end

  def to_a
    @array
  end

end

module Kernel

  def to_enum(method_name)
    array = []

    send(method_name) do |*values|
      array << values
    end

    Enumerator.new(array)
  end

end
