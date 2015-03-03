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

# Only part of Rubinius' string.rb

class String

  def include?(needle)
    !!find_string(StringValue(needle), 0)
  end

  def chars
    if block_given?
      each_char do |char|
        yield char
      end
    else
      each_char.to_a
    end
  end

  def chomp(separator=$/)
    str = dup
    str.chomp!(separator) || str
  end

  def slice!(one, two=undefined)
    Rubinius.check_frozen
    # This is un-DRY, but it's a simple manual argument splitting. Keeps
    # the code fast and clean since the sequence are pretty short.
    #
    if undefined.equal?(two)
      result = slice(one)

      if one.kind_of? Regexp
        lm = Regexp.last_match
        self[one] = '' if result
        Regexp.last_match = lm
      else
        self[one] = '' if result
      end
    else
      result = slice(one, two)

      if one.kind_of? Regexp
        lm = Regexp.last_match
        self[one, two] = '' if result
        Regexp.last_match = lm
      else
        self[one, two] = '' if result
      end
    end

    result
  end

  def each_line(sep=$/)
    return to_enum(:each_line, sep) unless block_given?

    # weird edge case.
    if sep.nil?
      yield self
      return self
    end

    sep = StringValue(sep)

    pos = 0

    size = @num_bytes
    orig_data = @data

    # If the separator is empty, we're actually in paragraph mode. This
    # is used so infrequently, we'll handle it completely separately from
    # normal line breaking.
    if sep.empty?
      sep = "\n\n"
      pat_size = 2

      while pos < size
        nxt = find_string(sep, pos)
        break unless nxt

        while @data[nxt] == 10 and nxt < @num_bytes
          nxt += 1
        end

        match_size = nxt - pos

        # string ends with \n's
        break if pos == @num_bytes

        str = byteslice pos, match_size
        yield str unless str.empty?

        # detect mutation within the block
        if !@data.equal?(orig_data) or @num_bytes != size
          raise RuntimeError, "string modified while iterating"
        end

        pos = nxt
      end

      # No more separates, but we need to grab the last part still.
      fin = byteslice pos, @num_bytes - pos
      yield fin if fin and !fin.empty?

    else

      # This is the normal case.
      pat_size = sep.size
      unmodified_self = clone

      while pos < size
        nxt = unmodified_self.find_string(sep, pos)
        break unless nxt

        match_size = nxt - pos
        str = unmodified_self.byteslice pos, match_size + pat_size
        yield str unless str.empty?

        pos = nxt + pat_size
      end

      # No more separates, but we need to grab the last part still.
      fin = unmodified_self.byteslice pos, @num_bytes - pos
      yield fin unless fin.empty?
    end

    self
  end

  def lines(sep=$/)
    if block_given?
      each_line(sep) do |line|
        yield line
      end
    else
      each_line(sep).to_a
    end
  end

  def gsub(pattern, replacement=undefined)
    # Because of the behavior of $~, this is duplicated from gsub! because
    # if we call gsub! from gsub, the last_match can't be updated properly.

    unless valid_encoding?
      raise ArgumentError, "invalid byte sequence in #{encoding}"
    end

    if undefined.equal? replacement
      unless block_given?
        return to_enum(:gsub, pattern, replacement)
      end
      use_yield = true
      tainted = false
    else
      tainted = replacement.tainted?
      untrusted = replacement.untrusted?

      unless replacement.kind_of?(String)
        hash = Rubinius::Type.check_convert_type(replacement, Hash, :to_hash)
        replacement = StringValue(replacement) unless hash
        tainted ||= replacement.tainted?
        untrusted ||= replacement.untrusted?
      end
      use_yield = false
    end

    pattern = Rubinius::Type.coerce_to_regexp(pattern, true) unless pattern.kind_of? Regexp
    match = pattern.search_region(self, 0, @num_bytes, true)

    unless match
      Regexp.last_match = nil
    end

    orig_len = @num_bytes
    orig_data = @data

    last_end = 0
    offset = nil

    last_match = nil

    ret = byteslice(0, 0) # Empty string and string subclass
    offset = match.full.at(0) if match

    while match
      if str = match.pre_match_from(last_end)
        ret.append str
      end

      if use_yield || hash
        Regexp.last_match = match

        if use_yield
          val = yield match.to_s
        else
          val = hash[match.to_s]
        end
        untrusted = true if val.untrusted?
        val = val.to_s unless val.kind_of?(String)

        tainted ||= val.tainted?

        ret.append val

        if !@data.equal?(orig_data) or @num_bytes != orig_len
          raise RuntimeError, "string modified"
        end
      else
        replacement.to_sub_replacement(ret, match)
      end

      tainted ||= val.tainted?

      last_end = match.full.at(1)

      if match.collapsing?
        if char = find_character(offset)
          offset += char.bytesize
        else
          offset += 1
        end
      else
        offset = match.full.at(1)
      end

      last_match = match

      match = pattern.match_from self, offset
      break unless match

      offset = match.full.at(0)
    end

    Regexp.last_match = last_match

    str = byteslice(last_end, @num_bytes-last_end+1)
    if str
      ret.append str
    end

    ret.taint if tainted
    ret.untrust if untrusted

    ret
  end

  def gsub!(pattern, replacement=undefined)
    # Because of the behavior of $~, this is duplicated from gsub! because
    # if we call gsub! from gsub, the last_match can't be updated properly.

    unless valid_encoding?
      raise ArgumentError, "invalid byte sequence in #{encoding}"
    end

    if undefined.equal? replacement
      unless block_given?
        return to_enum(:gsub, pattern, replacement)
      end
      Rubinius.check_frozen
      use_yield = true
      tainted = false
    else
      Rubinius.check_frozen
      tainted = replacement.tainted?
      untrusted = replacement.untrusted?

      unless replacement.kind_of?(String)
        hash = Rubinius::Type.check_convert_type(replacement, Hash, :to_hash)
        replacement = StringValue(replacement) unless hash
        tainted ||= replacement.tainted?
        untrusted ||= replacement.untrusted?
      end
      use_yield = false
    end

    pattern = Rubinius::Type.coerce_to_regexp(pattern, true) unless pattern.kind_of? Regexp
    match = pattern.search_region(self, 0, @num_bytes, true)

    unless match
      Regexp.last_match = nil
      return nil
    end

    orig_len = @num_bytes
    orig_data = @data

    last_end = 0
    offset = nil

    last_match = nil

    ret = byteslice(0, 0) # Empty string and string subclass
    offset = match.full.at(0)

    while match
      if str = match.pre_match_from(last_end)
        ret.append str
      end

      if use_yield || hash
        Regexp.last_match = match

        if use_yield
          val = yield match.to_s
        else
          val = hash[match.to_s]
        end
        untrusted = true if val.untrusted?
        val = val.to_s unless val.kind_of?(String)

        tainted ||= val.tainted?

        ret.append val

        if !@data.equal?(orig_data) or @num_bytes != orig_len
          raise RuntimeError, "string modified"
        end
      else
        replacement.to_sub_replacement(ret, match)
      end

      tainted ||= val.tainted?

      last_end = match.full.at(1)

      if match.collapsing?
        if char = find_character(offset)
          offset += char.bytesize
        else
          offset += 1
        end
      else
        offset = match.full.at(1)
      end

      last_match = match

      match = pattern.match_from self, offset
      break unless match

      offset = match.full.at(0)
    end

    Regexp.last_match = last_match

    str = byteslice(last_end, @num_bytes-last_end+1)
    if str
      ret.append str
    end

    ret.taint if tainted
    ret.untrust if untrusted

    replace(ret)
    self
  end


  def to_sub_replacement(result, match)
    index = 0
    while index < @num_bytes
      current = index
      while current < @num_bytes && @data[current] != 92  # ?\\
        current += 1
      end
      result.append(byteslice(index, current - index))
      break if current == @num_bytes

      # found backslash escape, looking next
      if current == @num_bytes - 1
        result.append("\\") # backslash at end of string
        break
      end
      index = current + 1

      cap = @data[index]

      additional = case cap
                     when 38   # ?&
                       match[0]
                     when 96   # ?`
                       match.pre_match
                     when 39   # ?'
                       match.post_match
                     when 43   # ?+
                       match.captures.compact[-1].to_s
                     when 48..57   # ?0..?9
                       match[cap - 48].to_s
                     when 92 # ?\\ escaped backslash
                       '\\'
                     when 107 # \k named capture
                       if @data[index + 1] == 60
                         name = ""
                         i = index + 2
                         while i < @data.size && @data[i] != 62
                           name << @data[i]
                           i += 1
                         end
                         if i >= @data.size
                           '\\'.append(cap.chr)
                           index += 1
                           next
                         end
                         index = i
                         name.force_encoding result.encoding
                         match[name]
                       else
                         '\\'.append(cap.chr)
                       end
                     else     # unknown escape
                       '\\'.append(cap.chr)
                   end
      result.append(additional)
      index += 1
    end
  end

  def sub(pattern, replacement=undefined)
    # Because of the behavior of $~, this is duplicated from sub! because
    # if we call sub! from sub, the last_match can't be updated properly.

    unless valid_encoding?
      raise ArgumentError, "invalid byte sequence in #{encoding}"
    end

    if undefined.equal? replacement
      unless block_given?
        return to_enum(:sub, pattern, replacement)
      end
      use_yield = true
      tainted = false
    else
      tainted = replacement.tainted?
      untrusted = replacement.untrusted?

      unless replacement.kind_of?(String)
        hash = Rubinius::Type.check_convert_type(replacement, Hash, :to_hash)
        replacement = StringValue(replacement) unless hash
        tainted ||= replacement.tainted?
        untrusted ||= replacement.untrusted?
      end
      use_yield = false
    end

    pattern = Rubinius::Type.coerce_to_regexp(pattern, true) unless pattern.kind_of? Regexp
    match = pattern.match_from(self, 0)

    Regexp.last_match = match

    ret = byteslice(0, 0) # Empty string and string subclass

    if match
      ret.append match.pre_match

      if use_yield || hash
        Regexp.last_match = match

        if use_yield
          val = yield match.to_s
        else
          val = hash[match.to_s]
        end
        untrusted = true if val.untrusted?
        val = val.to_s unless val.kind_of?(String)

        tainted ||= val.tainted?

        ret.append val
      else
        replacement.to_sub_replacement(ret, match)
      end

      ret.append(match.post_match)
      tainted ||= val.tainted?
    else
      return self
    end

    ret.taint if tainted
    ret.untrust if untrusted

    ret
  end

  def sub!(pattern, replacement=undefined)
    # Because of the behavior of $~, this is duplicated from sub! because
    # if we call sub! from sub, the last_match can't be updated properly.

    unless valid_encoding?
      raise ArgumentError, "invalid byte sequence in #{encoding}"
    end

    if undefined.equal? replacement
      unless block_given?
        return to_enum(:sub, pattern, replacement)
      end
      Rubinius.check_frozen
      use_yield = true
      tainted = false
    else
      Rubinius.check_frozen
      tainted = replacement.tainted?
      untrusted = replacement.untrusted?

      unless replacement.kind_of?(String)
        hash = Rubinius::Type.check_convert_type(replacement, Hash, :to_hash)
        replacement = StringValue(replacement) unless hash
        tainted ||= replacement.tainted?
        untrusted ||= replacement.untrusted?
      end
      use_yield = false
    end

    pattern = Rubinius::Type.coerce_to_regexp(pattern, true) unless pattern.kind_of? Regexp
    match = pattern.match_from(self, 0)

    Regexp.last_match = match

    ret = byteslice(0, 0) # Empty string and string subclass

    if match
      ret.append match.pre_match

      if use_yield || hash
        Regexp.last_match = match

        if use_yield
          val = yield match.to_s
        else
          val = hash[match.to_s]
        end
        untrusted = true if val.untrusted?
        val = val.to_s unless val.kind_of?(String)

        tainted ||= val.tainted?

        ret.append val
      else
        replacement.to_sub_replacement(ret, match)
      end

      ret.append(match.post_match)
      tainted ||= val.tainted?
    else
      return nil
    end

    ret.taint if tainted
    ret.untrust if untrusted

    replace(ret)
    self
  end

  def start_with?(*prefixes)
    prefixes.each do |original_prefix|
      prefix = Rubinius::Type.check_convert_type original_prefix, String, :to_str
      unless prefix
        raise TypeError, "no implicit conversion of #{original_prefix.class} into String"
      end
      return true if self[0, prefix.length] == prefix
    end
    false
  end

  def self.try_convert(obj)
    Rubinius::Type.try_convert obj, String, :to_str
  end

end
