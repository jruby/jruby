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

# Default Ruby Record Separator
# Used in this file and by various methods that need to ignore $/
DEFAULT_RECORD_SEPARATOR = "\n"

class String
  include Comparable

  ##
  # Creates a new string from copying _count_ bytes from the
  # _start_ of _bytes_.
  def self.from_bytearray(bytes, start, count)
    Rubinius.primitive :string_from_bytearray
    raise PrimitiveFailure, "String.from_bytearray primitive failed"
  end

  def self.try_convert(obj)
    Rubinius::Type.try_convert obj, String, :to_str
  end

  def %(args)
    *args = args
    ret = Rubinius::Sprinter.get(self).call(*args)

    ret.taint if tainted?
    return ret
  end

  def =~(pattern)
    case pattern
    when Regexp
      match_data = pattern.search_region(self, 0, bytesize, true)
      Regexp.last_match = match_data
      return match_data.begin(0) if match_data
    when String
      raise TypeError, "type mismatch: String given"
    else
      pattern =~ self
    end
  end

  def [](index, other = undefined)
    Rubinius.primitive :string_aref

    unless undefined.equal?(other)
      if index.kind_of?(Fixnum) && other.kind_of?(Fixnum)
        return substring(index, other)
      elsif index.kind_of? Regexp
        match, str = subpattern(index, other)
        Regexp.last_match = match
        return str
      else
        length = Rubinius::Type.coerce_to(other, Fixnum, :to_int)
        start  = Rubinius::Type.coerce_to(index, Fixnum, :to_int)
        return substring(start, length)
      end
    end

    case index
    when Regexp
      match_data = index.search_region(self, 0, bytesize, true)
      Regexp.last_match = match_data
      if match_data
        result = match_data.to_s
        Rubinius::Type.infect result, index
        return result
      end
    when String
      return include?(index) ? index.dup : nil
    when Range
      start   = Rubinius::Type.coerce_to index.first, Fixnum, :to_int
      length  = Rubinius::Type.coerce_to index.last,  Fixnum, :to_int

      start += size if start < 0

      length += size if length < 0
      length += 1 unless index.exclude_end?

      return "" if start == size
      return nil if start < 0 || start > size

      length = size if length > size
      length = length - start
      length = 0 if length < 0

      return substring(start, length)
    else
      index = Rubinius::Type.coerce_to index, Fixnum, :to_int
      return self[index]
    end
  end
  alias_method :slice, :[]

  def chomp(separator=$/)
    str = dup
    str.chomp!(separator) || str
  end

  def chop
    str = dup
    str.chop! || str
  end

  def delete(*strings)
    str = dup
    str.delete!(*strings) || str
  end

  def downcase
    return dup if bytesize == 0
    transform(Rubinius::CType::Lowered)
  end

  def end_with?(*suffixes)
    suffixes.each do |suffix|
      suffix = Rubinius::Type.check_convert_type suffix, String, :to_str
      next unless suffix

      return true if self[-suffix.length, suffix.length] == suffix
    end
    false
  end

  def include?(needle)
    !!find_string(StringValue(needle), 0)
  end

  def lstrip
    str = dup
    str.lstrip! || str
  end

  def oct
    to_inum(-8, false)
  end

  # Treats leading characters from <i>self</i> as a string of hexadecimal digits
  # (with an optional sign and an optional <code>0x</code>) and returns the
  # corresponding number. Zero is returned on error.
  #
  #    "0x0a".hex     #=> 10
  #    "-1234".hex    #=> -4660
  #    "0".hex        #=> 0
  #    "wombat".hex   #=> 0
  def hex
    to_inum(16, false)
  end

  def reverse
    dup.reverse!
  end

  def partition(pattern=nil)
    return super() if pattern == nil && block_given?

    if pattern.kind_of? Regexp
      if m = pattern.match(self)
        Regexp.last_match = m
        return [m.pre_match, m.to_s, m.post_match]
      end
    else
      pattern = StringValue(pattern)
      if i = index(pattern)
        post_start = i + pattern.length
        post_len = size - post_start

        return [substring(0, i),
                pattern.dup,
                substring(post_start, post_len)]
      end
    end

    # Nothing worked out, this is the default.
    return [self, "", ""]
  end

  def rpartition(pattern)
    if pattern.kind_of? Regexp
      if m = pattern.search_region(self, 0, size, false)
        Regexp.last_match = m
        [m.pre_match, m[0], m.post_match]
      end
    else
      pattern = StringValue(pattern)
      if i = rindex(pattern)
        post_start = i + pattern.length
        post_len = size - post_start

        return [substring(0, i),
                pattern.dup,
                substring(post_start, post_len)]
      end

      # Nothing worked out, this is the default.
      return ["", "", self]
    end
  end

  def rstrip
    str = dup
    str.rstrip! || str
  end

  def scan(pattern)
    taint = tainted? || pattern.tainted?
    pattern = Rubinius::Type.coerce_to_regexp(pattern, true)
    index = 0

    last_match = nil

    if block_given?
      ret = self
    else
      ret = []
    end

    while match = pattern.match_from(self, index)
      fin = match.full.at(1)

      if match.collapsing?
        if char = find_character(fin)
          index = fin + char.bytesize
        else
          index = fin + 1
        end
      else
        index = fin
      end

      last_match = match
      val = (match.length == 1 ? match[0] : match.captures)
      val.taint if taint

      if block_given?
        Regexp.last_match = match
        yield(val)
      else
        ret << val
      end
    end

    Regexp.last_match = last_match
    return ret
  end

  def split(pattern=nil, limit=undefined)
    Rubinius::Splitter.split(self, pattern, limit)
  end

  def squeeze(*strings)
    str = dup
    str.squeeze!(*strings) || str
  end

  def start_with?(*prefixes)
    prefixes.each do |prefix|
      prefix = Rubinius::Type.check_convert_type prefix, String, :to_str
      next unless prefix
      return true if self[0, prefix.length] == prefix
    end
    false
  end

  def strip
    str = dup
    str.strip! || str
  end

  def strip!
    left = lstrip!
    right = rstrip!
    left.nil? && right.nil? ? nil : self
  end

  def succ
    dup.succ!
  end

  def swapcase
    str = dup
    str.swapcase! || str
  end

  alias_method :intern, :to_sym

  def to_i(base=10)
    base = Rubinius::Type.coerce_to base, Integer, :to_int

    if base < 0 || base == 1 || base > 36
      raise ArgumentError, "illegal radix #{base}"
    end

    to_inum(base, false)
  end

  def tr(source, replacement)
    str = dup
    str.tr!(source, replacement) || str
  end

  def tr_s(source, replacement)
    str = dup
    str.tr_s!(source, replacement) || str
  end

  def upcase
    str = dup
    str.upcase! || str
  end

  def to_sub_replacement(result, match)
    data = bytes
    index = 0
    while index < bytesize
      current = index
      while current < bytesize && data[current] != 92  # ?\\
        current += 1
      end
      result.append(byteslice(index, current - index))
      break if current == bytesize

      # found backslash escape, looking next
      if current == bytesize - 1
        result.append("\\") # backslash at end of string
        break
      end
      index = current + 1

      cap = data[index]

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
                  if data[index + 1] == 60
                    name = ""
                    i = index + 2
                    while i < bytesize && data[i] != 62
                      name << data[i]
                      i += 1
                    end
                    if i >= bytesize
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

  def to_inum(base, check)
    Rubinius.primitive :string_to_inum
    raise ArgumentError, "invalid value for Integer"
  end

  def apply_and!(other)
    Rubinius.primitive :string_apply_and
    raise PrimitiveFailure, "String#apply_and! primitive failed"
  end

  def compare_substring(other, start, size)
    Rubinius.primitive :string_compare_substring

    if start > bytesize || start + bytesize < 0
      raise IndexError, "index #{start} out of string"
    end
    raise PrimitiveFailure, "String#compare_substring primitive failed"
  end
  
  def subpattern(pattern, capture)
    match = pattern.match(self)

    return nil unless match

    if index = Rubinius::Type.check_convert_type(capture, Fixnum, :to_int)
      return nil if index >= match.size || -index >= match.size
      capture = index
    end

    str = match[capture]
    Rubinius::Type.infect str, pattern
    [match, str]
  end
  private :subpattern

  def prefix?(other)
    size = other.size
    return false if size > bytesize
    other.compare_substring(self, 0, size) == 0
  end

  def suffix?(other)
    size = other.size
    return false if size > bytesize
    other.compare_substring(self, -size, size) == 0
  end

  def shorten!(size)
    self.modify!
    return if bytesize == 0
    self.num_bytes -= size
  end

  def each_codepoint
    return to_enum(:each_codepoint) { size } unless block_given?

    each_char { |c| yield c.ord }
    self
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

  def codepoints
    if block_given?
      each_codepoint do |codepoint|
        yield codepoint
      end
    else
      each_codepoint.to_a
    end
  end

  def encode!(to=undefined, from=undefined, options=undefined)
    Rubinius.check_frozen

    case to
    when Encoding
      to_enc = to
    when Hash
      options = to
      to_enc = Encoding.default_internal
    when undefined
      to_enc = Encoding.default_internal
      return self unless to_enc
    else
      opts = Rubinius::Type::check_convert_type to, Hash, :to_hash

      if opts
        options = opts
        to_enc = Encoding.default_internal
      else
        to_enc = Rubinius::Type.try_convert_to_encoding to
      end
    end

    case from
    when undefined
      from_enc = encoding
    when Encoding
      from_enc = from
    when Hash
      options = from
      from_enc = encoding
    else
      opts = Rubinius::Type::check_convert_type from, Hash, :to_hash

      if opts
        options = opts
        from_enc = encoding
      else
        from_enc = Rubinius::Type.coerce_to_encoding from
      end
    end

    if undefined.equal? from_enc or undefined.equal? to_enc
      raise Encoding::ConverterNotFoundError, "undefined code converter (#{from} to #{to})"
    end

    case options
    when undefined
      options = 0
    when Hash
      # do nothing
    else
      options = Rubinius::Type.coerce_to options, Hash, :to_hash
    end

    if ascii_only? and from_enc.ascii_compatible? and to_enc and to_enc.ascii_compatible?
      force_encoding to_enc
    elsif to_enc and from_enc != to_enc
      ec = Encoding::Converter.new from_enc, to_enc, options
      dest = ""
      status = ec.primitive_convert self.dup, dest, nil, nil, ec.options
      raise ec.last_error unless status == :finished
      replace dest
    end

    # TODO: replace this hack with transcoders
    if options.kind_of? Hash
      case xml = options[:xml]
      when :text
        gsub!(/[&><]/, '&' => '&amp;', '>' => '&gt;', '<' => '&lt;')
      when :attr
        gsub!(/[&><"]/, '&' => '&amp;', '>' => '&gt;', '<' => '&lt;', '"' => '&quot;')
        insert(0, '"')
        insert(-1, '"')
      when nil
        # nothing
      else
        raise ArgumentError, "unexpected value for xml option: #{xml.inspect}"
      end

      if options[:universal_newline]
        gsub!(/\r\n|\r/, "\r\n" => "\n", "\r" => "\n")
      end
    end

    self
  end

  def encode(to=undefined, from=undefined, options=undefined)
    dup.encode! to, from, options
  end

  def end_with?(*suffixes)
    suffixes.each do |original_suffix|
      suffix = Rubinius::Type.check_convert_type original_suffix, String, :to_str
      unless suffix
        raise TypeError, "no implicit conversion of #{original_suffix.class} into String"
      end
      return true if self[-suffix.length, suffix.length] == suffix
    end
    false
  end

  def inspect
    result_encoding = Encoding.default_internal || Encoding.default_external
    unless result_encoding.ascii_compatible?
      result_encoding = Encoding::US_ASCII
    end

    enc = encoding
    ascii = enc.ascii_compatible?
    enc_name = enc.name
    unicode = enc_name.start_with?("UTF-") && enc_name[4] != ?7

    if unicode
      if enc.equal? Encoding::UTF_16
        a = getbyte 0
        b = getbyte 1

        if a == 0xfe and b == 0xff
          enc = Encoding::UTF_16BE
        elsif a == 0xff and b == 0xfe
          enc = Encoding::UTF_16LE
        else
          unicode = false
        end
      elsif enc.equal? Encoding::UTF_32
        a = getbyte 0
        b = getbyte 1
        c = getbyte 2
        d = getbyte 3

        if a == 0 and b == 0 and c == 0xfe and d == 0xfe
          enc = Encoding::UTF_32BE
        elsif a == 0xff and b == 0xfe and c == 0 and d == 0
          enc = Encoding::UTF_32LE
        else
          unicode = false
        end
      end
    end

    array = []

    index = 0
    total = bytesize
    while index < total
      char = chr_at index

      if char
        bs = char.bytesize

        if (ascii or unicode) and bs == 1
          escaped = nil

          byte = getbyte(index)
          if byte >= 7 and byte <= 92
            case byte
            when 7  # \a
              escaped = '\a'
            when 8  # \b
              escaped = '\b'
            when 9  # \t
              escaped = '\t'
            when 10 # \n
              escaped = '\n'
            when 11 # \v
              escaped = '\v'
            when 12 # \f
              escaped = '\f'
            when 13 # \r
              escaped = '\r'
            when 27 # \e
              escaped = '\e'
            when 34 # \"
              escaped = '\"'
            when 35 # #
              case getbyte(index += 1)
              when 36   # $
                escaped = '\#$'
              when 64   # @
                escaped = '\#@'
              when 123  # {
                escaped = '\#{'
              else
                index -= 1
              end
            when 92 # \\
              escaped = '\\\\'
            end

            if escaped
              array << escaped
              index += 1
              next
            end
          end
        end

        if char.printable?
          array << char
        else
          code = char.ord
          escaped = code.to_s(16).upcase

          if unicode
            if code < 0x10000
              pad = "0" * (4 - escaped.bytesize)
              array << "\\u#{pad}#{escaped}"
            else
              array << "\\u{#{escaped}}"
            end
          else
            if code < 0x100
              pad = "0" * (2 - escaped.bytesize)
              array << "\\x#{pad}#{escaped}"
            else
              array << "\\x{#{escaped}}"
            end
          end
        end

        index += bs
      else
        array << "\\x#{getbyte(index).to_s(16)}"
        index += 1
      end
    end

    size = array.inject(0) { |s, chr| s += chr.bytesize }
    result = String.pattern size + 2, ?".ord
    m = Rubinius::Mirror.reflect result

    index = 1
    array.each do |chr|
      m.copy_from chr, 0, chr.bytesize, index
      index += chr.bytesize
    end

    Rubinius::Type.infect result, self
    result.force_encoding result_encoding
  end

  def prepend(other)
    self[0, 0] = other
    self
  end

  def upto(stop, exclusive=false)
    return to_enum :upto, stop, exclusive unless block_given?
    stop = StringValue(stop)

    if stop.size == 1 && size == 1
      return self if self > stop
      after_stop = stop.getbyte(0) + (exclusive ? 0 : 1)
      current = getbyte(0)
      until current == after_stop
        yield current.chr
        current += 1
      end
    else
      unless stop.size < size
        after_stop = exclusive ? stop : stop.succ
        current = self

        until current == after_stop
          yield current
          current = StringValue(current.succ)
          break if current.size > stop.size || current.size == 0
        end
      end
    end
    self
  end

  def sub(pattern, replacement=undefined)
    # Because of the behavior of $~, this is duplicated from sub! because
    # if we call sub! from sub, the last_match can't be updated properly.

    unless valid_encoding?
      raise ArgumentError, "invalid byte sequence in #{encoding}"
    end

    if undefined.equal? replacement
      unless block_given?
        raise ArgumentError, "method '#{__method__}': given 1, expected 2"
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
        raise ArgumentError, "method '#{__method__}': given 1, expected 2"
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

  alias_method :next, :succ
  alias_method :next!, :succ!

  def to_c
    Complexifier.new(self).convert
  end

  def to_r
    Rationalizer.new(self).convert
  end

  def chop!
    Rubinius.check_frozen

    m = Rubinius::Mirror.reflect self

    bytes = m.previous_byte_index bytesize
    return unless bytes

    chr = chr_at bytes
    if chr.ord == 10 and chr.ascii?
      if i = m.previous_byte_index(bytes)
        chr = chr_at i

        bytes = i if chr.ord == 13 and chr.ascii?
      end
    end

    self.num_bytes = bytes

    self
  end

  def chomp!(sep=undefined)
    Rubinius.check_frozen

    if undefined.equal?(sep)
      sep = $/
    elsif sep
      sep = StringValue(sep)
    end

    return if sep.nil?

    m = Rubinius::Mirror.reflect self

    if sep == DEFAULT_RECORD_SEPARATOR
      return unless bytes = m.previous_byte_index(bytesize)

      chr = chr_at bytes
      return unless chr.ascii?

      case chr.ord
      when 13
        # do nothing
      when 10
        if j = m.previous_byte_index(bytes)
          chr = chr_at j

          if chr.ord == 13 and chr.ascii?
            bytes = j
          end
        end
      else
        return
      end
    elsif sep.size == 0
      return if bytesize == 0
      bytes = bytesize

      while i = m.previous_byte_index(bytes)
        chr = chr_at i
        break unless chr.ord == 10 and chr.ascii?

        bytes = i

        if j = m.previous_byte_index(i)
          chr = chr_at j
          if chr.ord == 13 and chr.ascii?
            bytes = j
          end
        end
      end

      return if bytes == bytesize
    else
      size = sep.size
      return if size > bytesize

      # TODO: Move #compare_substring to mirror.
      return unless sep.compare_substring(self, -size, size) == 0
      bytes = bytesize - size
    end

    self.num_bytes = bytes

    self
  end

  def <<(other)
    Rubinius.check_frozen

    unless other.kind_of? String
      if other.kind_of? Integer
        if encoding == Encoding::US_ASCII and other >= 128 and other < 256
          force_encoding(Encoding::ASCII_8BIT)
        end

        other = other.chr(encoding)
      else
        other = StringValue(other)
      end
    end

    Rubinius::Type.infect(self, other)
    append(other)
  end
  alias_method :concat, :<<

  def chr
    substring 0, 1
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

    duped = dup

    # If the separator is empty, we're actually in paragraph mode. This
    # is used so infrequently, we'll handle it completely separately from
    # normal line breaking.
    if sep.empty?
      sep = "\n\n"
      pat_size = 2
      data = bytes

      while pos < size
        nxt = find_string(sep, pos)
        break unless nxt

        while data[nxt] == 10 and nxt < bytesize
          nxt += 1
        end

        match_size = nxt - pos

        # string ends with \n's
        break if pos == bytesize

        str = byteslice pos, match_size
        yield str unless str.empty?

        # detect mutation within the block
        if duped != self
          raise RuntimeError, "string modified while iterating"
        end

        pos = nxt
      end

      # No more separates, but we need to grab the last part still.
      fin = byteslice pos, bytesize - pos
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
      fin = unmodified_self.byteslice pos, bytesize - pos
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
    match = pattern.search_region(self, 0, bytesize, true)

    unless match
      Regexp.last_match = nil
    end

    duped = dup

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

        if duped != self
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

    str = byteslice(last_end, bytesize-last_end+1)
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
    match = pattern.search_region(self, 0, bytesize, true)

    unless match
      Regexp.last_match = nil
      return nil
    end

    duped = dup

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

        if duped != dup
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

    str = byteslice(last_end, bytesize-last_end+1)
    if str
      ret.append str
    end

    ret.taint if tainted
    ret.untrust if untrusted

    replace(ret)
    self
  end

  def match(pattern, pos=0)
    pattern = Rubinius::Type.coerce_to_regexp(pattern) unless pattern.kind_of? Regexp

    result = if block_given?
      pattern.match self, pos do |match|
        yield match
      end
    else
      pattern.match self, pos
    end
    Regexp.propagate_last_match
    result
  end

  # Removes invalid byte sequences from a String, available since Ruby 2.1.
  def scrub(replace = nil)
    output = ''
    input  = dup

    # The default replacement character is the "Unicode replacement" character.
    # (U+FFFD).
    if !replace and !block_given?
      replace = "\xEF\xBF\xBD".force_encoding("UTF-8")
        .encode(self.encoding, :undef => :replace, :replace => '?')
    end

    if replace
      unless replace.is_a?(String)
        raise(
          TypeError,
          "no implicit conversion of #{replace.class} into String"
        )
      end

      unless replace.valid_encoding?
        raise(
          ArgumentError,
          "replacement must be a valid byte sequence '#{replace.inspect}'"
        )
      end

      replace = replace.force_encoding(Encoding::BINARY)
    end

    # MRI appears to just return a copy of self when the input encoding is
    # BINARY/ASCII_8BIT.
    if input.encoding == Encoding::BINARY
      return input
    end

    converter = Encoding::Converter.new(input.encoding, Encoding::BINARY)

    while input.length > 0
      result = converter.primitive_convert(input, output, output.length)

      if result == :finished
        break
      elsif result == :undefined_conversion
        output << converter.primitive_errinfo[3]
      else
        # Blocks can return strings in any encoding so we'll make sure it's the
        # same as our buffer for the mean time.
        if block_given?
          block_output = yield(converter.primitive_errinfo[3])

          output << block_output.force_encoding(output.encoding)
        else
          output << replace
        end
      end
    end

    return output.force_encoding(encoding)
  end

  def scrub!(replace = nil, &block)
    replace(scrub(replace, &block))
    return self
  end

  def []=(index, count_or_replacement, replacement=undefined)
    Rubinius.check_frozen

    if undefined.equal?(replacement)
      replacement = count_or_replacement
      count = nil
    else
      count = count_or_replacement
    end

    m = Rubinius::Mirror.reflect self

    case index
    when Fixnum
      index += size if index < 0

      if index < 0 or index > size
        raise IndexError, "index #{index} out of string"
      end

      unless bi = m.byte_index(index)
        raise IndexError, "unable to find character at: #{index}"
      end

      if count
        count = Rubinius::Type.coerce_to count, Fixnum, :to_int

        if count < 0
          raise IndexError, "count is negative"
        end

        total = index + count
        if total >= size
          bs = bytesize - bi
        else
          bs = m.byte_index(total) - bi
        end
      else
        bs = index == size ? 0 : m.byte_index(index + 1) - bi
      end

      replacement = StringValue replacement
      enc = Rubinius::Type.compatible_encoding self, replacement

      m.splice bi, bs, replacement, enc
    when String
      unless start = m.byte_index(index)
        raise IndexError, "string not matched"
      end

      replacement = StringValue replacement
      enc = Rubinius::Type.compatible_encoding self, replacement

      m.splice start, index.bytesize, replacement, enc
    when Range
      start = Rubinius::Type.coerce_to index.first, Fixnum, :to_int

      start += size if start < 0

      if start < 0 or start > size
        raise RangeError, "#{index.first} is out of range"
      end

      unless bi = m.byte_index(start)
        raise IndexError, "unable to find character at: #{start}"
      end

      stop = Rubinius::Type.coerce_to index.last, Fixnum, :to_int
      stop += size if stop < 0
      stop -= 1 if index.exclude_end?

      if stop < start
        bs = 0
      elsif stop >= size
        bs = bytesize - bi
      else
        bs = m.byte_index(stop + 1) - bi
      end

      replacement = StringValue replacement
      enc = Rubinius::Type.compatible_encoding self, replacement

      m.splice bi, bs, replacement, enc
    when Regexp
      if count
        count = Rubinius::Type.coerce_to count, Fixnum, :to_int
      else
        count = 0
      end

      if match = index.match(self)
        ms = match.size
      else
        raise IndexError, "regexp does not match"
      end

      count += ms if count < 0 and -count < ms
      unless count < ms and count >= 0
        raise IndexError, "index #{count} out of match bounds"
      end

      unless match[count]
        raise IndexError, "regexp group #{count} not matched"
      end

      replacement = StringValue replacement
      enc = Rubinius::Type.compatible_encoding self, replacement

      bi = m.byte_index match.begin(count)
      bs = m.byte_index(match.end(count)) - bi

      m.splice bi, bs, replacement, enc
    else
      index = Rubinius::Type.coerce_to index, Fixnum, :to_int

      if count
        return self[index, count] = replacement
      else
        return self[index] = replacement
      end
    end

    Rubinius::Type.infect self, replacement

    return replacement
  end

  def center(width, padding=" ")
    padding = StringValue(padding)
    raise ArgumentError, "zero width padding" if padding.size == 0

    enc = Rubinius::Type.compatible_encoding self, padding

    width = Rubinius::Type.coerce_to width, Fixnum, :to_int
    return dup if width <= size

    width -= size
    left = width / 2

    bs = bytesize
    pbs = padding.bytesize

    if pbs > 1
      ps = padding.size
      pm = Rubinius::Mirror.reflect padding

      x = left / ps
      y = left % ps

      lpbi = pm.byte_index(y)
      lbytes = x * pbs + lpbi

      right = left + (width & 0x1)

      x = right / ps
      y = right % ps

      rpbi = pm.byte_index(y)
      rbytes = x * pbs + rpbi

      pad = self.class.pattern rbytes, padding
      str = self.class.pattern lbytes + bs + rbytes, ""
      m = Rubinius::Mirror.reflect str

      m.copy_from self, 0, bs, lbytes
      m.copy_from pad, 0, lbytes, 0
      m.copy_from pad, 0, rbytes, lbytes + bs
    else
      str = self.class.pattern width + bs, padding
      m = Rubinius::Mirror.reflect str
      m.copy_from self, 0, bs, left
    end

    str.taint if tainted? or padding.tainted?
    str.force_encoding enc
  end

  def ljust(width, padding=" ")
    padding = StringValue(padding)
    raise ArgumentError, "zero width padding" if padding.size == 0

    enc = Rubinius::Type.compatible_encoding self, padding

    width = Rubinius::Type.coerce_to width, Fixnum, :to_int
    return dup if width <= size

    width -= size

    bs = bytesize
    pbs = padding.bytesize

    if pbs > 1
      ps = padding.size
      pm = Rubinius::Mirror.reflect padding

      x = width / ps
      y = width % ps

      pbi = pm.byte_index(y)
      bytes = x * pbs + pbi

      str = self.class.pattern bytes + bs, self
      m = Rubinius::Mirror.reflect str

      i = 0
      bi = bs

      while i < x
        m.copy_from padding, 0, pbs, bi

        bi += pbs
        i += 1
      end

      m.copy_from padding, 0, pbi, bi
    else
      str = self.class.pattern width + bs, padding
      m = Rubinius::Mirror.reflect str

      m.copy_from self, 0, bs, 0
    end

    str.taint if tainted? or padding.tainted?
    str.force_encoding enc
  end

  def rjust(width, padding=" ")
    padding = StringValue(padding)
    raise ArgumentError, "zero width padding" if padding.size == 0

    enc = Rubinius::Type.compatible_encoding self, padding

    width = Rubinius::Type.coerce_to width, Fixnum, :to_int
    return dup if width <= size

    width -= size

    bs = bytesize
    pbs = padding.bytesize

    if pbs > 1
      ps = padding.size
      pm = Rubinius::Mirror.reflect padding

      x = width / ps
      y = width % ps

      bytes = x * pbs + pm.byte_index(y)
    else
      bytes = width
    end

    str = self.class.pattern bytes + bs, padding
    m = Rubinius::Mirror.reflect str

    m.copy_from self, 0, bs, bytes

    str.taint if tainted? or padding.tainted?
    str.force_encoding enc
  end

  def index(str, start=undefined)
    if undefined.equal?(start)
      start = 0
    else
      start = Rubinius::Type.coerce_to start, Fixnum, :to_int

      start += size if start < 0
      return if start < 0 or start > size
    end

    if str.kind_of? Regexp
      Rubinius::Type.compatible_encoding self, str

      m = Rubinius::Mirror.reflect self
      start = m.character_to_byte_index start
      if match = str.match_from(self, start)
        Regexp.last_match = match
        return match.begin(0)
      else
        Regexp.last_match = nil
        return
      end
    end

    str = StringValue(str)
    return start if str == ""

    Rubinius::Type.compatible_encoding self, str

    return if str.size > size

    m = Rubinius::Mirror.reflect self
    m.character_index str, start
  end

  def rindex(sub, finish=undefined)
    if undefined.equal?(finish)
      finish = size
    else
      finish = Rubinius::Type.coerce_to(finish, Integer, :to_int)
      finish += size if finish < 0
      return nil if finish < 0
      finish = size if finish >= size
    end

    m = Rubinius::Mirror.reflect self
    byte_finish = m.character_to_byte_index finish

    case sub
    when Fixnum
      if finish == size
        return nil if finish == 0
        finish -= 1
      end

      begin
        str = sub.chr
      rescue RangeError
        return nil
      end

      if byte_index = find_string_reverse(str, byte_finish)
        return m.byte_to_character_index byte_index
      end

    when Regexp
      Rubinius::Type.compatible_encoding self, sub

      match_data = sub.search_region(self, 0, byte_finish, false)
      Regexp.last_match = match_data
      return match_data.begin(0) if match_data

    else
      needle = StringValue(sub)
      needle_size = needle.size

      # needle is bigger that haystack
      return nil if size < needle_size

      # Boundary case
      return finish if needle_size == 0

      Rubinius::Type.compatible_encoding self, needle
      if byte_index = find_string_reverse(needle, byte_finish)
        return m.byte_to_character_index byte_index
      end
    end

    return nil
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

end
