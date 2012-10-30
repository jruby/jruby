class IOStub < String
  def write(*str)
    self << str.join
  end

  def print(*str)
    write(str.join + $\.to_s)
  end

  def puts(*str)
    write(str.collect { |s| s.to_s.chomp }.concat([nil]).join("\n"))
  end

  def printf(format, *args)
    self << sprintf(format, *args)
  end

  def flush
    self
  end
end

class Object
  # Creates a "bare" file descriptor (i.e. one that is not associated
  # with any Ruby object). The file descriptor can safely be passed
  # to IO.new without creating a Ruby object alias to the fd.
  def new_fd(name, mode="w:utf-8")
    IO.sysopen name, fmode(mode)
  end

  # Creates an IO instance for a temporary file name. The file
  # must be deleted.
  def new_io(name, mode="w:utf-8")
    IO.new new_fd(name, fmode(mode)), fmode(mode)
  end
end
