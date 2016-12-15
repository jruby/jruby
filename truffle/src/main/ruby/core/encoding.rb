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

class EncodingError < StandardError
end

class Encoding
  class Transcoding
    attr_reader :source, :target

    def initialize(source, target)
      @source = source
      @target = target
    end

    def inspect
      "#<#{super} #{source} to #{target}"
    end
  end

  class << self
    def build_encoding_map
      map = {}
      Encoding.list.each_with_index do |encoding, index|
        key = encoding.name.upcase.to_sym
        map[key] = [nil, index]
      end

      Truffle::Encoding.each_alias do |alias_name, index|
        key = alias_name.upcase.to_sym
        map[key] = [alias_name, index]
      end

      %w[internal external locale filesystem].each do |name|
        key = name.upcase.to_sym
        enc = Truffle::Encoding.get_default_encoding(name)
        index = enc ? map[enc.name.upcase.to_sym].last : nil
        map[key] = [name, index]
      end
      map
    end
    private :build_encoding_map

    def build_transcoding_map
      map = {}
      Encoding::Converter.each_transcoder do |source, destinations|
        h = {}
        destinations.each do |dest|
          h[dest] = Transcoding.new(source, dest)
        end
        map[source] = h
      end
      map
    end
    private :build_transcoding_map
  end

  TranscodingMap = build_transcoding_map
  EncodingMap = build_encoding_map

  @default_external = Truffle::Encoding.get_default_encoding('external')
  @default_internal = Truffle::Encoding.get_default_encoding('internal')

  class UndefinedConversionError < EncodingError
    attr_accessor :source_encoding_name
    attr_accessor :destination_encoding_name
    attr_accessor :source_encoding
    attr_accessor :destination_encoding
    attr_accessor :error_char

    private :source_encoding_name=
    private :destination_encoding_name=
    private :source_encoding=
    private :destination_encoding=
    private :error_char=
  end

  class InvalidByteSequenceError < EncodingError
    attr_accessor :source_encoding_name
    attr_accessor :destination_encoding_name
    attr_accessor :source_encoding
    attr_accessor :destination_encoding
    attr_accessor :error_bytes
    attr_accessor :readagain_bytes
    attr_writer :incomplete_input

    private :source_encoding_name=
    private :destination_encoding_name=
    private :source_encoding=
    private :destination_encoding=
    private :error_bytes=
    private :readagain_bytes=
    private :incomplete_input=

    def initialize(message="")
      super(message)

      @incomplete_input = nil
    end

    def incomplete_input?
      @incomplete_input
    end
  end

  class ConverterNotFoundError < EncodingError
  end

  class CompatibilityError < EncodingError
  end

  class Converter
    attr_accessor :source_encoding
    attr_accessor :destination_encoding
    attr_reader :replacement
    attr_reader :options

    def self.asciicompat_encoding(string_or_encoding)
      encoding = Rubinius::Type.try_convert_to_encoding string_or_encoding

      return unless encoding
      return if encoding.ascii_compatible?

      transcoding = TranscodingMap[encoding.name.upcase.to_sym]
      return unless transcoding and transcoding.size == 1

      Encoding.find transcoding.keys.first.to_s
    end

    def self.search_convpath(from, to, options=0)
      new(from, to, options).convpath
    end

    def initialize(from, to, options=0)
      @source_encoding = Rubinius::Type.coerce_to_encoding from
      @destination_encoding = Rubinius::Type.coerce_to_encoding to

      if options.kind_of? Fixnum
        @options = options
      else
        options = Rubinius::Type.coerce_to options, Hash, :to_hash

        @options = 0
        unless options.empty?
          @options |= INVALID_REPLACE if options[:invalid] == :replace
          @options |= UNDEF_REPLACE if options[:undef] == :replace

          if options[:newline] == :universal or options[:universal_newline]
            @options |= UNIVERSAL_NEWLINE_DECORATOR
          end

          if options[:newline] == :crlf or options[:crlf_newline]
            @options |= CRLF_NEWLINE_DECORATOR
          end

          if options[:newline] == :cr or options[:cr_newline]
            @options |= CR_NEWLINE_DECORATOR
          end

          @options |= XML_TEXT_DECORATOR if options[:xml] == :text
          if options[:xml] == :attr
            @options |= XML_ATTR_CONTENT_DECORATOR
            @options |= XML_ATTR_QUOTE_DECORATOR
          end

          replacement = options[:replace]
        end
      end

      source_name = @source_encoding.name.upcase.to_sym
      dest_name = @destination_encoding.name.upcase.to_sym

      unless source_name == dest_name
        @convpath, @converters = TranscodingPath[source_name, dest_name]
      end

      unless @convpath
        conversion = "(#{@source_encoding.name} to #{@destination_encoding.name})"
        msg = "code converter not found #{conversion}"
        raise ConverterNotFoundError, msg
      end

      if @options & (INVALID_REPLACE | UNDEF_REPLACE | UNDEF_HEX_CHARREF)
        if replacement.nil?
          if @destination_encoding == Encoding::UTF_8
            @replacement = "\xef\xbf\xbd".force_encoding(Encoding::UTF_8)
          else
            @replacement = "?".force_encoding(Encoding::US_ASCII)
          end
        else
          @replacement = Rubinius::Type.coerce_to replacement, String, :to_str
        end

        replacement_encoding_name = @replacement.encoding.name.upcase
        @replacement_converters = []

        @convpath.each do |enc|
          name = enc.to_s.upcase
          next if name == replacement_encoding_name

          _, converters = TranscodingPath[replacement_encoding_name.to_sym, enc]
          @replacement_converters << name << converters
        end
      end
      initialize_jruby(*[@source_encoding, @destination_encoding, @options])
    end

    def convert(str)
      str = StringValue(str)

      dest = ""
      status = primitive_convert str.dup, dest, nil, nil, @options | PARTIAL_INPUT

      if status == :invalid_byte_sequence or
         status == :undefined_conversion or
         status == :incomplete_input
        raise last_error
      end

      if status == :finished
        raise ArgumentError, "converter already finished"
      end

      if status != :source_buffer_empty
        raise RuntimeError, "unexpected result of Encoding::Converter#primitive_convert: #{status}"
      end

      dest
    end

    def primitive_convert(source, target, offset=nil, size=nil, options=0)
      source = StringValue(source) if source
      target = StringValue(target)

      if offset.nil?
        offset = target.bytesize
      else
        offset = Rubinius::Type.coerce_to offset, Fixnum, :to_int
      end

      if size.nil?
        size = -1
      else
        size = Rubinius::Type.coerce_to size, Fixnum, :to_int

        if size < 0
          raise ArgumentError, "byte size is negative"
        end
      end

      if offset < 0
        raise ArgumentError, "byte offset is negative"
      end

      if offset > target.bytesize
        raise ArgumentError, "byte offset is greater than destination buffer size"
      end

      if !options.kind_of? Fixnum
        opts = Rubinius::Type.coerce_to options, Hash, :to_hash

        options = 0
        options |= PARTIAL_INPUT if opts[:partial_input]
        options |= AFTER_OUTPUT if opts[:after_output]
      end

      if primitive_errinfo.first == :invalid_byte_sequence
        source.prepend putback
      end

      Truffle.invoke_primitive(:encoding_converter_primitive_convert,
                                self, source, target, offset, size, options)
    end

    def putback(maxbytes=nil)
      Truffle.primitive :encoding_converter_putback

      putback maxbytes
    end

    def finish
      dest = ""
      status = primitive_convert nil, dest

      if status == :invalid_byte_sequence or
         status == :undefined_conversion or
         status == :incomplete_input
        raise last_error
      end

      if status != :finished
        raise RuntimeError, "unexpected result of Encoding::Converter#finish: #{status}"
      end

      dest
    end

    def last_error
      error = Truffle.invoke_primitive :encoding_converter_last_error, self
      return if error.nil?

      result, source_encoding_name, destination_encoding_name, error_bytes, read_again_bytes = error
      read_again_string = nil
      codepoint = nil
      error_bytes_msg = error_bytes.dump

      case result
      when :invalid_byte_sequence
        if read_again_string
          msg = "#{error_bytes_msg} followed by #{read_again_string.dump} on #{source_encoding_name}"
        else
          msg = "#{error_bytes_msg} on #{source_encoding_name}"
        end

        exc = InvalidByteSequenceError.new msg
      when :incomplete_input
        msg = "incomplete #{error_bytes_msg} on #{source_encoding_name}"

        exc = InvalidByteSequenceError.new msg
      when :undefined_conversion
        error_char = error_bytes
        if codepoint
          error_bytes_msg = "U+%04X" % codepoint
        end

        if source_encoding_name.to_sym == @source_encoding.name and
           destination_encoding_name.to_sym == @destination_encoding.name
          msg = "#{error_bytes_msg} from #{source_encoding_name} to #{destination_encoding_name}"
        else
          msg = "#{error_bytes_msg} to #{destination_encoding_name} in conversion from #{source_encoding_name}"
          transcoder = @converters.first
          msg << " to #{transcoder.target}"
        end

        exc = UndefinedConversionError.new msg
      end

      Truffle.privately do
        exc.source_encoding_name = source_encoding_name
        src = Rubinius::Type.try_convert_to_encoding source_encoding_name
        exc.source_encoding = src unless false == src

        exc.destination_encoding_name = destination_encoding_name
        dst = Rubinius::Type.try_convert_to_encoding destination_encoding_name
        exc.destination_encoding = dst unless false == dst

        if error_char
          error_char.force_encoding src unless false == src
          exc.error_char = error_char
        end

        if result == :invalid_byte_sequence or result == :incomplete_input
          exc.error_bytes = error_bytes.force_encoding Encoding::ASCII_8BIT

          if bytes = read_again_bytes
            exc.readagain_bytes = bytes.force_encoding Encoding::ASCII_8BIT
          end
        end

        if result == :invalid_byte_sequence
          exc.incomplete_input = false
        elsif result == :incomplete_input
          exc.incomplete_input = true
        end
      end

      exc
    end

    def primitive_errinfo
      Truffle.primitive :encoding_converter_primitive_errinfo
      raise PrimitiveFailure, "Encoding::Converter#primitive_errinfo primitive failed"
    end

    def convpath
      path = []
      a = 0
      b = @convpath.size - 1

      while a < b
        path << [Encoding.find(@convpath[a].to_s), Encoding.find(@convpath[a + 1].to_s)]
        a += 1
      end

      path << "xml_text_escape" if @options & XML_TEXT_DECORATOR != 0
      path << "xml_attr_content_escape" if @options & XML_ATTR_CONTENT_DECORATOR != 0
      path << "xml_attr_quote" if @options & XML_ATTR_QUOTE_DECORATOR != 0
      path << "universal_newline" if @options & UNIVERSAL_NEWLINE_DECORATOR != 0
      path << "crlf_newline" if @options & CRLF_NEWLINE_DECORATOR != 0
      path << "cr_newline" if @options & CR_NEWLINE_DECORATOR != 0

      path
    end

    def inspect
      "#<Encoding::Converter: #{source_encoding.name} to #{destination_encoding.name}>"
    end

    def replacement=(str)
      str = StringValue(str)

      @replacement = str.encode(@destination_encoding)
    end

    class TranscodingPath
      @paths = {}

      def self.[](source, target)
        key = "[#{source}, #{target}]"

        path, converters = @paths[key]

        unless path
          return unless path = search(source, target)
          @paths[key] = [path]
        end

        unless converters
          converters = get_converters path
          @paths[key][1] = converters
        end

        return path, converters
      end

      def self.search(source, target)
        if entry = TranscodingMap[source]
          if entry[target]
            return [source, target]
          else
            visited = { source => true }
            search = { [source] => entry }

            until search.empty?
              path, table = search.shift

              table.each do |key, _|
                next if visited.key? key
                next unless entry = TranscodingMap[key]

                return path << key << target if entry[target]

                unless visited.key? key
                  search[path.dup << key] = entry
                  visited[key] = true
                end
              end
            end
          end
        end
      end

      def self.get_converters(path)
        converters = []
        total = path.size - 1
        i = 0

        while i < total
          entry = TranscodingMap[path[i]][path[i + 1]]
          converters << entry
          i += 1
        end

        converters
      end
    end
  end

  def self.aliases
    aliases = {}
    EncodingMap.each do |n, r|
      index = r.last
      next unless index

      aname = r.first
      aliases[aname] = Truffle.invoke_primitive(:encoding_get_encoding_by_index, index).name if aname
    end

    aliases
  end

  def self.set_alias_index(name, obj)
    key = name.upcase.to_sym

    case obj
    when Encoding
      source_name = obj.name
    when nil
      EncodingMap[key][1] = nil
      return
    else
      source_name = StringValue(obj)
    end

    entry = EncodingMap[source_name.upcase.to_sym]
    raise ArgumentError, "unknown encoding name - #{source_name}" unless entry
    index = entry.last

    EncodingMap[key][1] = index
  end
  private_class_method :set_alias_index

  def self.default_external
    @default_external
  end

  def self.default_external=(enc)
    raise ArgumentError, "default external encoding cannot be nil" if enc.nil?

    enc = find(enc)
    set_alias_index "external", enc
    set_alias_index "filesystem", enc
    @default_external = enc
    Truffle::Encoding.default_external = enc
  end

  def self.default_internal
    @default_internal
  end

  def self.default_internal=(enc)
    enc = find(enc) unless enc.nil?
    set_alias_index "internal", enc
    @default_internal = enc
    Truffle::Encoding.default_internal = enc
  end

  def self.find(name)
    enc = Rubinius::Type.try_convert_to_encoding name
    return enc unless false == enc

    raise ArgumentError, "unknown encoding name - #{name}"
  end

  def self.name_list
    EncodingMap.map do |n, r|
      index = r.last
      r.first or (index and Truffle.invoke_primitive(:encoding_get_encoding_by_index, index).name)
    end
  end

  def inspect
    "#<Encoding:#{name}#{" (dummy)" if dummy?}>"
  end

  def names
    entry = EncodingMap[name.upcase.to_sym]
    names = [name]
    EncodingMap.each do |k, r|
      aname = r.first
      names << aname if aname and r.last == entry.last
    end
    names
  end

  def replicate(name)
    Truffle.invoke_primitive(:encoding_replicate, self, StringValue(name))
  end

  def _dump(depth)
    name
  end

  def self._load(name)
    find name
  end

end

Encoding::TranscodingMap[:'UTF-16BE'] = {}
Encoding::TranscodingMap[:'UTF-16BE'][:'UTF-8'] = nil

Encoding::TranscodingMap[:'UTF-16LE'] = {}
Encoding::TranscodingMap[:'UTF-16LE'][:'UTF-8'] = nil

Encoding::TranscodingMap[:'UTF-32BE'] = {}
Encoding::TranscodingMap[:'UTF-32BE'][:'UTF-8'] = nil

Encoding::TranscodingMap[:'UTF-32LE'] = {}
Encoding::TranscodingMap[:'UTF-32LE'][:'UTF-8'] = nil

Encoding::TranscodingMap[:'ISO-2022-JP'] = {}
Encoding::TranscodingMap[:'ISO-2022-JP'][:'STATELESS-ISO-2022-JP'] = nil
