class StringIO
  attr_accessor :pos

  def initialize(string=String.new)
    @string = string
    @close_read = false
    @close_write = false
    @pos = 0
    @eof = false
  end

  def close() close_read; close_write; end
  def closed?() closed_read? && closed_write?; end
  def close_read() @close_read = true; end
  def closed_read?() @close_read; end
  def close_write() @close_write = true; end
  def closed_write?() @close_write; end

  def each(sep=$/)
    line = gets
    while line != nil
      yield(line)
      line = gets
    end
  end
  def each_byte() @string[@pos..-1].each_byte; end
  def each_line() each; end
  def eof() @pos = length + 1; end
  def eof?() @pos > length; end
  def getc() 
    c = @string[@pos]
    @pos = @pos + 1
    c
  end
  def gets(sep=$/)
    i = @string.index(sep, @pos)
    if i.nil?
      @pos = length + 1
	  return nil
    end
    line = @string[@pos...i+1]
    @pos = i + sep.length
    line
  end
  def length() @string.length; end
  def print(obj=$_, *rest)
  	@string = @string + obj.to_s
  	rest.each { |arg| @string = @string + arg.to_s }
  	@string = @string + $\
  end
  def printf(*args) @string = @string + sprintf(*args); end
  def puts(*args) @string = @string + args.join("\n") + "\n" end
  def read(*args)
    str = nil
    
    raise ArgumentError.new("wrong number of arguments (#{args.length} for 0)") if args.length > 2

    str = args[1] if args.length == 2

    skip = false
    if (args.length >= 1)
      if (!args[0].nil?)
        length = olength = args[0]
        raise ArgumentError.new("negative length #{length} given") if length < 0
        
        if (length > 0 && @pos >= @string.length)
          @eof = true
      	  replace_string_reference_with str, "" unless str.nil?
          return nil
        elsif @eof
      	  replace_string_reference_with str, "" unless str.nil?
          return nil
        end
        skip = true
      end
    end
    
    if (args.length >= 0 && !skip)
      olength = -1
      length = @string.length
      if (length <= @pos)
      	@eof = true
      	if !str
      	  str = ""
      	else
      	  replace_string_reference_with str, ""
      	end
      	
      	return str
      else
        length = length - @pos
      end
    end

    if str.nil?
      str = @string[@pos, length]
      str = "" if str.nil?
    else
      rest = @string.length - @pos
      if (length > rest)
        length = rest
      end
      replace_string_reference_with str, @string[@pos, length]
    end
    if str.nil?
      str = "" if @eof
      length = 0
    else
      @pos = @pos + length
    end

    @eof = true if olength < 0 || olength > length
    str
  end
  alias readline gets
  def rewind() @pos = 0; end
  def seek(amount, whence=IO::SEEK_SET)
    if (whence == IO::SEEK_CUR)
      @pos = @pos + amount
    elsif (whence == IO::SEEK_END)
      @pos = length + amount
    else
      @pos = amount
    end
  end
  def string; @string; end
  def syswrite(s); @string << s; s.length; end
  def write(s); @string = s; end
  private
  def replace_string_reference_with(s, new); s.gsub! /^.*/, new; end
end
