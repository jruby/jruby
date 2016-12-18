# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
# OTHER DEALINGS IN THE SOFTWARE.

module Rubinius
class ByteArray

  def self.from_string(string)
    new(0, 0).append(string)
  end

  def to_str
    String.from_bytearray(self, 0, size)
  end

  def chomp!(value)
    if self[size - 1] == value
      return truncate(size - 1)
    end

    nil
  end

  def unpack(format)
    to_str.unpack(format)
  end
end
end