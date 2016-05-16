# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

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

module Rubinius
  class Mirror
    class Range < Mirror
      self.subject = ::Range

      def step_float_iterations_size(first, last, step_size)
        err = (first.abs + last.abs + (last - first).abs) / step_size.abs * Float::EPSILON
        err = 0.5 if err > 0.5

        if excl
          iterations = ((last - first) / step_size - err).floor
          iterations += 1 if iterations * step_size + first < last
        else
          iterations = ((last - first) / step_size + err).floor + 1
        end

        iterations
      end

      def step_iterations_size(first, last, step_size)
        case first
        when Float
          step_float_iterations_size(first, last, step_size)
        else
          @object.size.nil? ? nil : (@object.size.fdiv(step_size)).ceil
        end
      end

      def validate_step_size(first, last, step_size)
        if step_size.kind_of? Float or first.kind_of? Float or last.kind_of? Float
          # if any are floats they all must be
          begin
            step_size = Float(from = step_size)
            first     = Float(from = first)
            last      = Float(from = last)
          rescue ArgumentError
            raise TypeError, "no implicit conversion to float from #{from.class}"
          end
        else
          step_size = Integer(from = step_size)

          unless step_size.kind_of? Integer
            raise TypeError, "can't convert #{from.class} to Integer (#{from.class}#to_int gives #{step_size.class})"
          end
        end

        if step_size <= 0
          raise ArgumentError, "step can't be negative" if step_size < 0
          raise ArgumentError, "step can't be 0"
        end

        return first, last, step_size
      end

      def excl
        @object.exclude_end?
      end

    end
  end
end
