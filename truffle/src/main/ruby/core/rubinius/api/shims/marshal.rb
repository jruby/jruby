# Copyright (c) 2007-2015, Evan Phoenix and contributors
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

# Modifications are subject to:
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Exception

  # Custom marshal dumper for Exception. Rubinius exposes the exception message as an instance variable and their
  # dumper takes advantage of that. This dumper instead calls Exception#message to get the message, but is otherwise
  # identical.
  def __marshal__(ms)
    out = ms.serialize_extended_object self
    out << "o"
    cls = Rubinius::Type.object_class self
    name = Rubinius::Type.module_inspect cls
    out << ms.serialize(name.to_sym)
    out << ms.serialize_fixnum(2)

    out << ms.serialize(:mesg)
    out << ms.serialize(self.message)
    out << ms.serialize(:bt)
    out << ms.serialize(self.backtrace)

    out
  end

end

class Range

  # Custom marshal dumper for Range. Rubinius exposes the three main values in Range (begin, end, excl) as
  # instance variables. MRI does not, but the values are encoded as instance variables within the marshal output from
  # MRI, so they both generate the same output, with the exception of the ordering of the variables. In JRuby+Truffle,
  # we do something more along the lines of MRI and as such, the default Rubinius handler for dumping Range doesn't
  # work for us because there are no instance variables to dump. This custom dumper explicitly encodes the three main
  # values so we generate the correct dump data.
  def __marshal__(ms)
    out = ms.serialize_extended_object self
    out << "o"
    cls = Rubinius::Type.object_class self
    name = Rubinius::Type.module_inspect cls
    out << ms.serialize(name.to_sym)
    out << ms.serialize_integer(3 + self.instance_variables.size)
    out << ms.serialize(:begin)
    out << ms.serialize(self.begin)
    out << ms.serialize(:end)
    out << ms.serialize(self.end)
    out << ms.serialize(:excl)
    out << ms.serialize(self.exclude_end?)
    out << ms.serialize_instance_variables_suffix(self, true, true)
  end

end

module Marshal
  class State

    def construct_object
      name = get_symbol
      klass = const_lookup name, Class

      if klass <= Range
        construct_range(klass)
      else
        obj = klass.allocate

        raise TypeError, 'dump format error' unless Object === obj

        store_unique_object obj
        if Rubinius::Type.object_kind_of? obj, Exception
          set_exception_variables obj
        else
          set_instance_variables obj
        end

        obj
      end
    end

    # Rubinius stores three main values in Range (begin, end, excl) as instance variables and as such, can use the
    # normal, generic object deserializer. In JRuby+Truffle, we do not expose these values as instance variables, in
    # keeping with MRI. Moreover, we have specialized versions of Ranges depending on these values, so changing them
    # after object construction would create optimization problems. Instead, we patch the Rubinius marshal loader here
    # to specifically handle Ranges by constructing a Range of the proper type using the deserialized main values and
    # then setting any custom instance variables afterward.
    def construct_range(klass)
      range_begin = nil
      range_end = nil
      range_exclude_end = false
      ivars = {}

      construct_integer.times do
        ivar = prepare_ivar(get_symbol)
        value = construct

        case ivar
          when :@begin then range_begin = value
          when :@end then range_end = value
          when :@excl then range_exclude_end = value
          else ivars[ivar] = value
        end
      end

      range = klass.new(range_begin, range_end, range_exclude_end)
      store_unique_object range

      ivars.each { |name, value| range.__instance_variable_set__ name, value }

      range
    end

  end
end
