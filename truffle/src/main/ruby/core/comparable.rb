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

module Comparable
  def ==(other)
    return true if equal?(other)

    return false if Thread.detect_recursion(self, other) do
      unless comp = (self <=> other)
        return false
      end

      return Comparable.compare_int(comp) == 0
    end
  end

  def >(other)
    unless comp = (self <=> other)
      raise ArgumentError, "comparison of #{self.class} with #{other.class}"
    end

    Comparable.compare_int(comp) > 0
  end

  def >=(other)
    unless comp = (self <=> other)
      raise ArgumentError, "comparison of #{self.class} with #{other.class}"
    end

    Comparable.compare_int(comp) >= 0
  end

  def <(other)
    unless comp = (self <=> other)
      raise ArgumentError, "comparison of #{self.class} with #{other.class}"
    end

    Comparable.compare_int(comp) < 0
  end

  def <=(other)
    unless comp = (self <=> other)
      raise ArgumentError, "comparison of #{self.class} with #{other.class}"
    end

    Comparable.compare_int(comp) <= 0
  end

  def between?(min, max)
    # This could be more elegant, but we need to use <= and => on self to match
    # MRI.
    return false if self < min
    return false if self > max
    return true
  end

  # A version of MRI's rb_cmpint (sort of)
  def self.compare_int(int)
    return int if int.kind_of? Fixnum

    return 1  if int > 0
    return -1 if int < 0
    return 0
  end
end

