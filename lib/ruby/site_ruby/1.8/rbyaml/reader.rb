# This is a more or less straight translation of PyYAML3000 to Ruby

# the big difference in this implementation is that unicode support is not here...

require 'rbyaml/error'

module RbYAML

  # Reader:
  # - checks if characters are in allowed range,
  # - adds '\0' to the end.
  # Reader accepts
  #  - a String object
  #  - a duck-typed IO object
  module Reader
    def initialize_reader(stream)
      @stream = nil
      @stream_pointer = 0
      @eof = true
      @buffer = ""
      @pointer = 0
      @index = 0
      @line = 0
      @column = 0
      if String === stream
        @name = "<string>"
        @raw_buffer = stream
      else
        @stream = stream
        @name = stream.respond_to?(:path) ? stream.path : stream.inspect
        @eof = false
        @raw_buffer = ""
      end
    end

    def peek(index=0)
      update(index+1) if @pointer+index+1 >= @buffer.length
      @buffer[@pointer+index]
    end
    
    def prefix(length=1)
      update(length) if @pointer+length >= @buffer.length
      @buffer[@pointer...@pointer+length]
    end

    def forward(length=1)
      update(length+1) if @pointer+length+1 >= @buffer.length
      length.times { |k|
        ch = @buffer[@pointer]
        @pointer += 1
        @index += 1
        if "\n\x85".include?(ch) || (ch == ?\r && @buffer[@pointer+1] != ?\n)
          @line += 1
          @column = 0
        else
          @column += 1
        end
      }
    end
    
    def get_mark
      if @stream.nil?
        Mark.new(@name,@index,@line,@column,@buffer,@pointer)
      else
        Mark.new(@name,@index,@line,@column,nil,nil)
      end
    end
    
    NON_PRINTABLE = /[^\x09\x0A\x0D\x20-\x7E\x85\xA0-\xFF]/
    def check_printable(data)
      if NON_PRINTABLE =~ data
        position = @index+@buffer.length-@pointer+($~.offset(0)[0])
        raise ReaderError.new(@name, position, $&,"unicode","special characters are not allowed"),"special characters are not allowed"
      end
    end

    def update(length)
      return if @raw_buffer.nil?
      @buffer = @buffer[@pointer..-1]
      @pointer = 0
      while @buffer.length < length
        unless @eof
          update_raw
        end
        data = @raw_buffer
        converted = data.length
        check_printable(data)
        @buffer << data
        @raw_buffer = @raw_buffer[converted..-1]
        if @eof
          @buffer << ?\0
          @raw_buffer = nil
          break
        end
      end
    end

    def update_raw(size=1024)
      data = @stream.read(size)
      if data && !data.empty?
        @raw_buffer << data
        @stream_pointer += data.length
      else
        @eof = true
      end
    end
  end

  class ReaderError < YAMLError
    def initialize(name, position, character, encoding, reason)
      @name = name
      @position = position
      @character = character
      @encoding = encoding
      @reason = reason
    end

    def to_s
      if String === @character
        "'#{@encoding}' codec can't decode byte #x%02x: #{@reason}\n  in \"#{@name}\", position #{@position}" % @character.to_i
      else
        "unacceptable character #x%04x: #{@reason}\n  in \"#{@name}\", position #{@position}" % @character.to_i
      end
    end
  end
end

