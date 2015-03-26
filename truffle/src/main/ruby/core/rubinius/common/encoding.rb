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

class EncodingError < StandardError
end

class Encoding
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

  class Transcoding
    attr_accessor :source
    attr_accessor :target

    def inspect
      "#<#{super} #{source} to #{target}"
    end
  end

  class Converter
    attr_accessor :source_encoding
    attr_accessor :destination_encoding
    attr_reader :replacement
    attr_reader :options

    def self.allocate
      Rubinius.primitive :encoding_converter_allocate
      raise PrimitiveFailure, "Encoding::Converter.allocate primitive failed"
    end

    def self.asciicompat_encoding(string_or_encoding)
      encoding = Rubinius::Type.try_convert_to_encoding string_or_encoding

      return if not encoding or undefined.equal? encoding
      return if encoding.ascii_compatible?

      transcoding = TranscodingMap[encoding.name.upcase]
      return unless transcoding and transcoding.size == 1

      Encoding.find transcoding.keys.first.to_s
    end

    def self.search_convpath(from, to, options=undefined)
      new(from, to, options).convpath
    end

    def initialize(from, to, options=undefined)
      @source_encoding = Rubinius::Type.coerce_to_encoding from
      @destination_encoding = Rubinius::Type.coerce_to_encoding to

      if options.kind_of? Fixnum
        @options = options
      elsif !undefined.equal? options
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
      else
        @options = 0
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

          _, converters = TranscodingPath[replacement_encoding_name, enc]
          @replacement_converters << name << converters
        end
      end
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

      Rubinius.invoke_primitive(:encoding_converter_primitive_convert,
                                self, source, target, offset, size, options)
    end

    def putback(maxbytes=nil)
      Rubinius.primitive :encoding_converter_putback

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
      error = Rubinius.invoke_primitive :encoding_converter_last_error, self
      return if error.nil?

      result = error[:result]
      error_bytes = error[:error_bytes]
      error_bytes_msg = error_bytes.dump
      source_encoding_name = error[:source_encoding_name]
      destination_encoding_name = error[:destination_encoding_name]

      case result
        when :invalid_byte_sequence
          read_again_string = error[:read_again_string]
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
          if codepoint = error[:codepoint]
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

      Rubinius.privately do
        exc.source_encoding_name = source_encoding_name
        src = Rubinius::Type.try_convert_to_encoding source_encoding_name
        exc.source_encoding = src unless undefined.equal? src

        exc.destination_encoding_name = destination_encoding_name
        dst = Rubinius::Type.try_convert_to_encoding destination_encoding_name
        exc.destination_encoding = dst unless undefined.equal? dst

        if error_char
          error_char.force_encoding src unless undefined.equal? src
          exc.error_char = error_char
        end

        if result == :invalid_byte_sequence or result == :incomplete_input
          exc.error_bytes = error_bytes.force_encoding Encoding::ASCII_8BIT

          if bytes = error[:read_again_bytes]
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
      Rubinius.primitive :encoding_converter_primitive_errinfo
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
      @load_cache = true
      @cache_valid = false
      @transcoders_count = TranscodingMap.size

      def self.paths
        if load_cache? and cache_threshold?
          begin
            path = "#{Rubinius::RUNTIME_PATH}/delta/converter_paths"
            Rubinius::CodeLoader.require_compiled path
            cache_loaded
          rescue Object
            disable_cache
          end
        end

        @paths
      end

      def self.disable_cache
        @cache_valid = false
        @load_cache = false
      end

      def self.cache_loaded
        @cache_valid = true
        @load_cache = false
      end

      def self.load_cache?
        @load_cache
      end

      def self.cache_loaded?
        @load_cache == false and @cache_valid
      end

      def self.cache_threshold?
        @paths.size > 5
      end

      def self.default_transcoders?
        @transcoders_count == TranscodingMap.size
      end

      def self.cache_valid?
        cache_loaded? and default_transcoders?
      end

      def self.[](source, target)
        key = "[#{source}, #{target}]"

        path, converters = paths[key]

        unless path
          return if cache_valid?
          return unless path = search(source, target)
          paths[key] = [path]
        end

        unless converters
          converters = get_converters path
          paths[key][1] = converters
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

          if entry.kind_of? String
            lib = "#{Rubinius::ENC_PATH}/#{entry}"
            Rubinius::NativeMethod.load_extension lib, entry

            entry = TranscodingMap[path[i]][path[i + 1]]
          end

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
      aliases[aname] = EncodingList[index].name if aname
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
    if undefined.equal? @default_external
      @default_external = find "external"
    end
    @default_external
  end

  def self.default_external=(enc)
    raise ArgumentError, "default external encoding cannot be nil" if enc.nil?

    set_alias_index "external", enc
    set_alias_index "filesystem", enc
    @default_external = undefined
  end

  def self.default_internal
    if undefined.equal? @default_internal
      @default_internal = find "internal"
    end
    @default_internal
  end

  def self.default_internal=(enc)
    set_alias_index "internal", enc
    @default_internal = undefined
  end

  def self.find(name)
    enc = Rubinius::Type.try_convert_to_encoding name
    return enc unless undefined.equal? enc

    raise ArgumentError, "unknown encoding name - #{name}"
  end

  def self.list
    EncodingList
  end

  def self.locale_charmap
    LocaleCharmap
  end

  def self.name_list
    EncodingMap.map do |n, r|
      index = r.last
      r.first or (index and EncodingList[index].name)
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

  def _dump(depth)
    name
  end

  def self._load(name)
    find name
  end
end
