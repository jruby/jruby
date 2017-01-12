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

class Symbol
  include Comparable

  def <=>(other)
    return unless other.kind_of? Symbol

    to_s <=> other.to_s
  end

  def capitalize
    to_s.capitalize.to_sym
  end

  def casecmp(other)
    return unless other.kind_of? Symbol

    to_s.casecmp(other.to_s)
  end

  def downcase
    to_s.downcase.to_sym
  end

  ##
  # Returns a pretty version of the symbol, fit for viewing
  #  :symbol.inspect #=> ":symbol"
  def inspect
    str = to_s

    case str
    when /\A(\$|@@?)[a-z_][a-z_\d]*\z/i,                      # Variable names
         /\A[a-z_][a-z_\d]*[=?!]?\z/i,                        # Method names
         /\A\$(-[a-z_\d]|[+~:?<_\/'"$.,`!;\\=*>&@]|\d+)\z/i,  # Special global variables
         /\A([|^&\/%~`!]|!=|!~|<<|>>|<=>|===?|=~|[<>]=?|[+-]@?|\*\*?|\[\]=?)\z/ # Operators
      ":#{str}"
    else
      ":#{str.inspect}"
    end
  end

  def empty?
    to_s.empty?
  end

  alias_method :intern, :to_sym
  alias_method :id2name, :to_s

  def length
    to_s.length
  end

  alias_method :size, :length

  def match(pattern)
    str = to_s

    case pattern
    when Regexp
      match_data = pattern.search_region(str, 0, str.bytesize, true)
      Truffle.invoke_primitive(:regexp_set_last_match, match_data)
      return match_data.byte_begin(0) if match_data
    when String
      raise TypeError, "type mismatch: String given"
    else
      pattern =~ str
    end
  end

  alias_method :=~, :match

  def encoding
    Truffle.invoke_primitive :encoding_get_object_encoding, self
  end

  def swapcase
    to_s.swapcase.to_sym
  end

  def upcase
    to_s.upcase.to_sym
  end

  def succ
    to_s.succ.to_sym
  end

  alias_method :next, :succ

  def [](index, other = undefined)
    if index.kind_of?(Regexp)
      unless undefined.equal?(other)
        match, str = to_s.send(:subpattern, index, other)
        Truffle.invoke_primitive(:regexp_set_last_match, match)
        return str
      end

      str = to_s
      match_data = index.search_region(str, 0, str.bytesize, true)
      Truffle.invoke_primitive(:regexp_set_last_match, match_data)
      if match_data
        result = match_data.to_s
        Rubinius::Type.infect result, index
        return result
      end
    else
      to_s[index, other]
    end
  end

  alias_method :slice, :[]

  # Use equal? for ===
  alias_method :===, :equal?
end
