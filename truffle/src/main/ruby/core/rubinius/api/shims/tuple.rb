# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius

  class Tuple < Array

    def self.pattern(num, val)
       Tuple.new(num, val)
    end


  end

end


class Array

  def copy_from(other, start, length, dest)
    Rubinius.primitive :tuple_copy_from

    unless other.kind_of? Array # Rubinius::Tuple # We use Array
      raise TypeError, "Tuple#copy_from was expecting an Array, not a #{other.class}"
    end
    start = Rubinius::Type.coerce_to start, Fixnum, :to_i
    length = Rubinius::Type.coerce_to length, Fixnum, :to_i
    dest = Rubinius::Type.coerce_to dest, Fixnum, :to_i

    if start < 0 || start > other.size # other.fields
      raise IndexError, "Start %d is out of bounds %d" % [start, other.size]# other.fields]
    end

    if dest < 0 || dest > self.size # self.fields
      raise IndexError, "Destination %d is out of bounds %d" % [dest, self.size] # self.fields]
    end

    if length < 0
      raise IndexError, "length %d must be positive" % [length]
    end

    if (start + length) > other.size # other.fields
      raise IndexError, "end index %d can not exceed size of source %d" % [start+length, other.size]# other.fields]
    end

    if length > ( self.size - dest )#self.fields - dest)
      raise IndexError, "length %d can not exceed space %d in destination" % [length, self.size - dest]#self.fields - dest]
    end

    src = start
    dst = dest
    while src < (start + length)
      put dst, other.at(src)
      src += 1
      dst += 1
    end

    self
  end


  def swap(a, b)
    temp = at(a)
    self[a] = at(b)
    self[b] = temp
  end

  alias_method :put, :[]=

end
