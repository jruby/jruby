# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
#
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
  class Splitter
    def self.split_characters(string, pattern, limit, tail_empty)
      if limit
        string.chars.take(limit - 1) << (string.size > (limit - 1) ? string[(limit - 1)..-1] : "")
      else
        ret = string.chars.to_a
        # Use #byteslice because it returns the right class and taints
        # automatically. This is just appending a "", which is this
        # strange protocol if a negative limit is passed in
        ret << string.byteslice(0,0) if tail_empty
        ret
      end
    end

    def self.valid_encoding?(string)
      raise ArgumentError, "invalid byte sequence in #{string.encoding.name}" unless string.valid_encoding?
    end

    def self.split(string, pattern, limit)
      # Odd edge case
      return [] if string.empty?

      tail_empty = false

      if undefined.equal?(limit)
        limited = false
      else
        limit = Rubinius::Type.coerce_to limit, Fixnum, :to_int

        if limit > 0
          return [string.dup] if limit == 1
          limited = true
        else
          if limit < 0
            tail_empty = true
          end
          limited = false
        end
      end

      pattern ||= ($; || " ")

      if pattern == ' '
        if limited
          lim = limit
        elsif tail_empty
          lim = -1
        else
          lim = 0
        end

        return Truffle.invoke_primitive :string_awk_split, string, lim
      elsif pattern.kind_of?(Regexp)
      else
        pattern = StringValue(pattern) unless pattern.kind_of?(String)

        valid_encoding?(string)
        valid_encoding?(pattern) 

        trim_end = !tail_empty || limit == 0

        unless limited
          if pattern.empty?
            if trim_end
              return string.chars.to_a
            end
          else
            return split_on_string(string, pattern, trim_end)
          end
        end

        pattern = Regexp.new(Regexp.quote(pattern))
      end

      # Handle // as a special case.
      if pattern.source.empty?
        return split_characters(string, pattern, limited && limit, tail_empty)
      end

      start = 0
      ret = []

      last_match = nil
      last_match_end = 0

      while match = pattern.match_from(string, start)
        break if limited && limit - ret.size <= 1

        collapsed = match.collapsing?

        unless collapsed && (match.byte_begin(0) == last_match_end)
          ret << match.pre_match_from(last_match_end)

          # length > 1 means there are captures
          if match.length > 1
            ret.concat(match.captures.compact)
          end
        end

        start = match.byte_end(0)
        if collapsed
          start += 1
        end

        last_match = match
        last_match_end = last_match.byte_end(0)
      end

      if last_match
        ret << last_match.post_match
      elsif ret.empty?
        ret << string.dup
      end

      # Trim from end
      if undefined.equal?(limit) || limit == 0
        while s = ret.at(-1) and s.empty?
          ret.pop
        end
      end

      ret
    end

    def self.split_on_string(string, pattern, trim_end)
      pos = 0

      ret = []

      pat_size = pattern.bytesize
      str_size = string.bytesize

      while pos < str_size
        nxt = string.find_string(pattern, pos)
        break unless nxt

        match_size = nxt - pos
        ret << string.byteslice(pos, match_size)

        pos = nxt + pat_size
      end

      # No more separators, but we need to grab the last part still.
      ret << string.byteslice(pos, str_size - pos)

      if trim_end
        while s = ret.at(-1) and s.empty?
          ret.pop
        end
      end

      ret
    end
  end
end
