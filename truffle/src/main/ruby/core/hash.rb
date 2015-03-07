# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2014, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Hash

  def invert
    inverted = {}
    each_pair { |key, value|
      inverted[value] = key
    }
    inverted
  end

  def to_hash
    self
  end

  # Implementation of a fundamental Rubinius method that allows their Hash
  # implementation to work. We probably want to remove uses of this in the long
  # term, as it creates objects which already exist for them and we have to
  # create, but for now it allows us to use more Rubinius code unmodified.

  KeyValue = Struct.new(:key, :value)

  def each_item
    each do |key, value|
      yield KeyValue.new(key, value)
    end
    nil
  end

  def merge_fallback(other, &block)
    merge(Rubinius::Type.coerce_to other, Hash, :to_hash, &block)
  end

  def find_item(key)
    value = _get_or_undefined(key)
    if undefined.equal?(value)
      nil
    else
      # TODO CS 7-Mar-15 maybe we should return the stored key?
      KeyValue.new(key, value)
    end
  end

  # From Rubinius

  def self._construct_fallback(*args)
    if args.size == 1
      obj = args.first
      if hash = Rubinius::Type.check_convert_type(obj, Hash, :to_hash)
        new_hash = allocate.replace(hash)
        new_hash.default = nil
        return new_hash
      elsif associate_array = Rubinius::Type.check_convert_type(obj, Array, :to_ary)
        return new_from_associate_array(associate_array)
      end
    end

    return new if args.empty?

    if args.size & 1 == 1
      raise ArgumentError, "Expected an even number, got #{args.length}"
    end

    hash = new
    i = 0
    total = args.size

    while i < total
      hash[args[i]] = args[i+1]
      i += 2
    end

    hash
  end

end
