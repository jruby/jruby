class StringIO
  attr_accessor :pos

  def newInstance(string=String.new)
    @string = string
    @close_read = false
    @close_write = false
  end

  def close() close_read; close_write; end
  def closed?() closed_read? && closed_write?; end
  def close_read() @close_read = true; end
  def closed_read?() @close_read; end
  def close_write() @close_write = true; end
  def closed_write?() @close_write; end

  def each(sep="\n")
    line = gets
    while line != nil
      yield(line)
      line = gets
    end
  end
  def each_byte() @string.substr(@pos).each_byte; end
  def each_line() each; end
  def eof() @pos = length + 1; end
  def eof?() @pos > length; end
  def getc() 
    c = @string[@pos]
    @pos = @pos + 1
    c
  end
  def gets(sep="\n")
    i = @string.index(sep, @pos)
    @string.substr(@pos, i)
  end
  def length() @string.length; end
#  def putc(c) @string << c; end
  def print() @string.print; end
  def printf(*args) @string.printf(*args); end
#  def puts(s) @string << s; end
  alias readline gets
  def rewind() @pos = 0; end
#  def seek(amount, whence=IO::SEEK_SET)
#    if whence == IO::SEEK_CUR then
#      @pos = @pos + amount
#    else if whence == IO::SEEK_END then
#      @pos = length + amount
#    else
#      @pos = amount
#    end
#   end
#   alias size length
  def string; @string; end
  def write(s); puts "SSSS #{s}"; @string = s; end
end
