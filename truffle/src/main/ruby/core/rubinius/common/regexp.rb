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

class Regexp
  IGNORECASE         = 1
  EXTENDED           = 2
  MULTILINE          = 4
  FIXEDENCODING      = 16
  NOENCODING         = 32
  DONT_CAPTURE_GROUP = 128
  CAPTURE_GROUP      = 256

  KCODE_NONE = (1 << 9)
  KCODE_EUC  = (2 << 9)
  KCODE_SJIS = (3 << 9)
  KCODE_UTF8 = (4 << 9)
  KCODE_MASK = KCODE_NONE | KCODE_EUC | KCODE_SJIS | KCODE_UTF8

  OPTION_MASK = IGNORECASE | EXTENDED | MULTILINE | FIXEDENCODING | NOENCODING | DONT_CAPTURE_GROUP | CAPTURE_GROUP

  ESCAPE_TABLE = Rubinius::Tuple.new(256)

  # Seed it with direct replacements
  i = 0
  while i < 256
    ESCAPE_TABLE[i] = i.chr
    i += 1
  end

  # The character literals (?x) are Fixnums in 1.8 and Strings in 1.9
  # so we use literal values instead so this is 1.8/1.9 compatible.
  ESCAPE_TABLE[9]   = '\\t'
  ESCAPE_TABLE[10]  = '\\n'
  ESCAPE_TABLE[11]  = '\\v'
  ESCAPE_TABLE[12]  = '\\f'
  ESCAPE_TABLE[13]  = '\\r'
  ESCAPE_TABLE[32]  = '\\ '
  ESCAPE_TABLE[35]  = '\\#'
  ESCAPE_TABLE[36]  = '\\$'
  ESCAPE_TABLE[40]  = '\\('
  ESCAPE_TABLE[41]  = '\\)'
  ESCAPE_TABLE[42]  = '\\*'
  ESCAPE_TABLE[43]  = '\\+'
  ESCAPE_TABLE[45]  = '\\-'
  ESCAPE_TABLE[46]  = '\\.'
  ESCAPE_TABLE[63]  = '\\?'
  ESCAPE_TABLE[91]  = '\\['
  ESCAPE_TABLE[92]  = '\\\\'
  ESCAPE_TABLE[93]  = '\\]'
  ESCAPE_TABLE[94]  = '\\^'
  ESCAPE_TABLE[123] = '\\{'
  ESCAPE_TABLE[124] = '\\|'
  ESCAPE_TABLE[125] = '\\}'

  class << self
    alias_method :compile, :new
  end

  def self.try_convert(obj)
    Rubinius::Type.try_convert obj, Regexp, :to_regexp
  end

  def self.convert(pattern)
    return pattern if pattern.kind_of? Regexp
    if pattern.kind_of? Array
      return union(*pattern)
    else
      return Regexp.quote(pattern.to_s)
    end
  end

  def self.compatible?(*patterns)
    encodings = patterns.map{ |r| convert(r).encoding }
    last_enc = encodings.pop
    encodings.each do |encoding|
      raise ArgumentError, "incompatible encodings: #{encoding} and #{last_enc}" unless Encoding.compatible?(last_enc, encoding)
      last_enc = encoding
    end
  end

  def self.union(*patterns)
    case patterns.size
    when 0
      return %r/(?!)/
    when 1
      pat = patterns.first
      case pat
      when Array
        return union(*pat)
      when Regexp
        return pat
      else
        return Regexp.new(Regexp.quote(StringValue(pat)))
      end
    else
      compatible?(*patterns)
      enc = convert(patterns.first).encoding
    end

    str = "".encode(enc)
    sep = "|".encode(enc)
    patterns.each_with_index do |pat, idx|
      str << sep if idx != 0
      if pat.kind_of? Regexp
        str << pat.to_s
      else
        str << Regexp.quote(StringValue(pat))
      end
    end

    Regexp.new(str)
  end

  def self.escape(str)
    str = str.to_s if str.is_a?(Symbol)
    escaped = StringValue(str).transform(ESCAPE_TABLE)
    if escaped.ascii_only?
      escaped.force_encoding Encoding::US_ASCII
    elsif str.valid_encoding?
      escaped.force_encoding str.encoding
    else
      escaped.force_encoding Encoding::ASCII_8BIT
    end
  end

  class << self
    alias_method :quote, :escape
  end

  def initialize(pattern, opts=nil, lang=nil)
    if pattern.kind_of?(Regexp)
      opts = pattern.options
      pattern = pattern.source
    elsif pattern.kind_of?(Fixnum) or pattern.kind_of?(Float)
      raise TypeError, "can't convert Fixnum into String"
    elsif opts.kind_of?(Fixnum)
      opts = opts & (OPTION_MASK | KCODE_MASK) if opts > 0
    elsif opts
      opts = IGNORECASE
    else
      opts = 0
    end

    code = lang[0] if lang
    opts |= NOENCODING if code == ?n or code == ?N

    compile pattern, opts
  end

  def initialize_copy(other)
    initialize other.source, other.options
  end

  def match(str, pos=0)
    unless str
      Regexp.last_match = nil
      return nil
    end

    str = str.to_s if str.is_a?(Symbol)
    str = StringValue(str)

    m = Rubinius::Mirror.reflect str
    pos = pos < 0 ? pos + str.size : pos
    pos = m.character_to_byte_index pos
    result = search_region(str, pos, str.bytesize, true)
    Regexp.last_match = result

    if result && block_given?
      yield result
    else
      result
    end
  end

  # Returns the index of the first character in the region that
  # matched or nil if there was no match. See #match for returning
  # the MatchData instead.
  def =~(str)
    str = str.to_s if str.is_a?(Symbol)
    # unless str.nil? because it's nil and only nil, not false.
    str = StringValue(str) unless str.nil?

    match = match_from(str, 0)
    if match
      Regexp.last_match = match
      return match.begin(0)
    else
      Regexp.last_match = nil
      return nil
    end
  end

  def ===(other)
    if other.kind_of? Symbol
      other = other.to_s
    elsif !other.kind_of? String
      other = Rubinius::Type.check_convert_type other, String, :to_str
      unless other
        Regexp.last_match = nil
        return false
      end
    end

    if match = match_from(other, 0)
      Regexp.last_match = match
      true
    else
      Regexp.last_match = nil
      false
    end
  end

  def eql?(other)
    return false unless other.kind_of?(Regexp)
    return false unless source == other.source
    (options & ~NOENCODING) == (other.options & ~NOENCODING)
  end

  alias_method :==, :eql?

  def hash
    str = '/' << source << '/' << option_to_string(options)
    str.hash
  end

  def inspect
    # the regexp matches any / that is after anything except for a \
    escape = source.gsub(%r!(\\.)|/!) { $1 || '\/' }
    str = "/#{escape}/#{option_to_string(options)}"
    str << 'n' if (options & NOENCODING) > 0
    str
  end

  def encoding
    source.encoding
  end


  def source
    @source.dup
  end

  def ~
    line = $_

    unless line.kind_of?(String)
      Regexp.last_match = nil
      return nil
    end

    res = match(line)
    return res ? res.begin(0) : nil
  end

  def match_all(str)
    start = 0
    arr = []
    while match = self.match_from(str, start)
      arr << match
      if match.collapsing?
        start += 1
      else
        start = match.full.at(1)
      end
    end
    arr
  end

  def casefold?
    (options & IGNORECASE) > 0 ? true : false
  end

  def match_from(str, count)
    return nil unless str
    search_region(str, count, str.bytesize, true)
  end

  class SourceParser
    class Part
      OPTIONS_MAP = {
        'm' => Regexp::MULTILINE,
        'i' => Regexp::IGNORECASE,
        'x' => Regexp::EXTENDED
      }

      attr_accessor :options
      attr_accessor :source

      def initialize(source="")
        @source = source
        @options = []
        @negated_options = []
      end

      def <<(str)
        @source << str
      end

      def empty?
        @source.empty?
      end

      def flatten
      end

      def to_s
        # Put in the proper \'s to escape /'s
        # This is the same regexp used by #inspect
        source.gsub(%r!(\\.)|/!) { $1 || '\/' }
      end

      def has_options!
        @has_options = true
      end

      def has_options?
        @has_options
      end
    end

    class OptionsGroupPart < Part
      def to_s
        @flatten ? "#{source}" : "(#{options_string}#{source})"
      end

      def push_option!(identifier)
        @options << identifier
      end

      def push_negated_option!(identifier)
        @negated_options << identifier
      end

      def flatten
        @flatten = true
      end

      def options_string
        string = @options.join + (@negated_options.empty? ? "" : @negated_options.join)
        "?#{string}:"
      end
      private :options_string
    end

    class LookAheadGroupPart < Part
      def to_s
        "(#{source})"
      end
    end

    def initialize(source, options = 0)
      @source = source
      @options = options
      @parts = [Part.new]
    end


    def string
      "(?#{options_string}:#{parts_string})"
    end

    def parts_string
      if parts.size == 1 && parts.first.has_options?
        parts.first.flatten
      end
      parts.map { |part| part.to_s }.join
    end

    def parts
      return @parts if @already_parsed
      @index = 0
      create_parts
      @parts.reject! { |part| part.empty? }
      @already_parsed = true
      @parts
    end

    # TODO: audit specs for this method when specs are running
    def create_parts
      while @index < @source.size
        if @source[@index].chr == '('
          idx = @index + 1
          if idx < @source.size and @source[idx].chr == '?'
            process_group
          else
            push_current_character!
          end
        else
          push_current_character!
        end
      end
    end

    def process_group
      @index += 1
      @parts << group_part_class.new

      if in_group_with_options?
        @index += 1
        process_group_options
      end

      process_look_ahead if in_lookahead_group?
      process_until_group_finished
      add_part!
    end

    def group_part_class
      if in_group_with_options?
        OptionsGroupPart
      else
        LookAheadGroupPart
      end
    end

    def in_lookahead_group?
      @source[@index, 2] == "?=" || @source[@index, 2] == "?!"
    end

    def process_look_ahead
      push_current_character!
      push_current_character!
    end

    def in_group_with_options?
      return false if @source[@index, 1] != '?'

      @source[@index + 1..-1].each_byte do |b|
        c = b.chr
        return true if ':' == c
        return false unless %w[m i x -].include? c
      end
    end

    def process_group_options
      @parts.last.has_options!
      case @source[@index].chr
      when ')'
        return
      when ':'
        @index += 1
        return
      else
        push_option!
        process_group_options
      end
    end

    def process_until_group_finished
      if @source[@index].chr == ")"
        @index += 1
        return
      else
        push_current_character!
        process_until_group_finished
      end
    end

    def push_current_character!
      @parts.last << @source[@index].chr
      @index += 1
    end

    def push_option!
      @parts.last.push_option!(@source[@index].chr)
      @index += 1
    end

    def add_part!
      @parts << Part.new
    end

    PossibleOptions = [[MULTILINE, "m"], [IGNORECASE, "i"], [EXTENDED, "x"]]
    def options_string
      chosen_options = []
      possible_options = PossibleOptions
      possible_options.each do |flag, identifier|
        chosen_options << identifier if @options & flag > 0
      end

      if parts.size == 1
        chosen_options.concat parts.first.options
      end

      excluded_options = possible_options.map { |e| e.last }.select do |identifier|
        !chosen_options.include?(identifier)
      end

      options_to_return = chosen_options
      if !excluded_options.empty?
        options_to_return << "-" << excluded_options
      end
      options_to_return.join
    end

  end

  def to_s
    SourceParser.new(source, options).string
  end

  def option_to_string(option)
    string = ""
    string << 'm' if (option & MULTILINE) > 0
    string << 'i' if (option & IGNORECASE) > 0
    string << 'x' if (option & EXTENDED) > 0
    string
  end

  def name_table
    @names
  end

  #
  # call-seq:
  #    rxp.named_captures  => hash
  #
  # Returns a hash representing information about named captures of <i>rxp</i>.
  #
  # A key of the hash is a name of the named captures.
  # A value of the hash is an array which is list of indexes of corresponding
  # named captures.
  #
  #    /(?<foo>.)(?<bar>.)/.named_captures
  #    #=> {"foo"=>[1], "bar"=>[2]}
  #
  #    /(?<foo>.)(?<foo>.)/.named_captures
  #    #=> {"foo"=>[1, 2]}
  #
  # If there are no named captures, an empty hash is returned.
  #
  #    /(.)(.)/.named_captures
  #    #=> {}
  #
  def named_captures
    hash = {}

    if @names
      @names.sort_by { |a,b| b.first }.each do |k, v| # LookupTable is unordered
        hash[k.to_s] = v
      end
    end

    return hash
  end

  #
  # call-seq:
  #    rxp.names   => [name1, name2, ...]
  #
  # Returns a list of names of captures as an array of strings.
  #
  #     /(?<foo>.)(?<bar>.)(?<baz>.)/.names
  #     #=> ["foo", "bar", "baz"]
  #
  #     /(?<foo>.)(?<foo>.)/.names
  #     #=> ["foo"]
  #
  #     /(.)(.)/.names
  #     #=> []
  #
  def names
    if @names
      @names.sort_by { |a,b| b.first }.map { |x| x.first.to_s } # LookupTable is unordered
    else
      []
    end
  end

end

class MatchData

  def begin(idx)
    if idx == 0
      start = @full.at(0)
    else
      start = @region.at(idx - 1).at(0)
      return nil if start == -1
    end
    m = Rubinius::Mirror.reflect @source
    m.byte_to_character_index start
  end

  def end(idx)
    if idx == 0
      fin = @full.at(1)
    else
      fin = @region.at(idx - 1).at(1)
      return nil if fin == -1
    end
    m = Rubinius::Mirror.reflect @source
    m.byte_to_character_index fin
  end

  def offset(idx)
    out = []
    out << self.begin(idx)
    out << self.end(idx)
    return out
  end

  def [](idx, len = nil)
    return to_a[idx, len] if len

    case idx
    when Fixnum
      if idx <= 0
        return matched_area() if idx == 0
        return to_a[idx]
      elsif idx <= @region.size
        tup = @region[idx - 1]

        x = tup.at(0)
        return nil if x == -1

        y = tup.at(1)
        return @source.byteslice(x, y-x)
      end
    when String
      if @regexp.name_table
        return self[idx.to_sym]
      end
      raise IndexError, "Unknown named group '#{idx}'"
    when Symbol
      if @regexp.name_table
        if nums = @regexp.name_table[idx]
          nums.reverse_each do |num|
            val = self[num]
            return val if val
          end
          return nil
        end
      end
      raise IndexError, "Unknown named group '#{idx}'"
    end

    return to_a[idx]
  end

  attr_reader :regexp

  def ==(other)
    other.kind_of?(MatchData) &&
      string == other.string  &&
      regexp == other.regexp  &&
      captures == other.captures
  end
  alias_method :eql?, :==

  def string
    @source.dup.freeze
  end

  def source
    @source
  end

  def full
    @full
  end

  def length
    @region.fields + 1
  end

  def captures
    out = Array.new(@region.fields)

    idx = 0
    @region.each do |tup|
      x = tup.at(0)

      if x == -1
        val = nil
      else
        y = tup.at(1)
        val = @source.byteslice(x, y-x)
      end

      out[idx] = val
      idx += 1
    end

    return out
  end

  def names
    @regexp.names
  end

  def pre_match
    return @source.byteslice(0, 0) if @full.at(0) == 0
    nd = @full.at(0) - 1
    @source.byteslice(0, nd+1)
  end

  def pre_match_from(idx)
    return @source.byteslice(0, 0) if @full.at(0) == 0
    nd = @full.at(0) - 1
    @source.byteslice(idx, nd-idx+1)
  end

  def collapsing?
    @full[0] == @full[1]
  end

  def post_match
    nd = @source.bytesize - 1
    st = @full.at(1)
    @source.byteslice(st, nd-st+1)
  end

  def inspect
    capts = captures
    if capts.empty?
      "#<MatchData \"#{matched_area}\">"
    else
      idx = 0
      capts.map! { |capture| "#{idx += 1}:#{capture.inspect}" }
      "#<MatchData \"#{matched_area}\" #{capts.join(" ")}>"
    end
  end

  def select
    unless block_given?
      raise LocalJumpError, "no block given"
    end

    out = []
    ma = matched_area()
    out << ma if yield ma

    each_capture do |str|
      if yield(str)
        out << str
      end
    end
    return out
  end

  alias_method :size, :length

  def to_a
    ary = captures()
    ary.unshift matched_area()
    return ary
  end

  def values_at(*indexes)
    indexes.map { |i| self[i] }.flatten(1)
  end

  def matched_area
    x = @full.at(0)
    y = @full.at(1)
    @source.byteslice(x, y-x)
  end

  alias_method :to_s, :matched_area
  private :matched_area

  def get_capture(num)
    x, y = @region[num]
    return nil if !y or x == -1

    return @source.byteslice(x, y-x)
  end

  private :get_capture

  def each_capture
    @region.each do |tup|
      x, y = *tup
      yield @source.byteslice(x, y-x)
    end
  end

  private :each_capture

end

