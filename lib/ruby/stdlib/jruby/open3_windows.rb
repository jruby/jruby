# This is a modified Open3 that overwrites methods not supported on Windows
# with versions that work.

module Open3

  def popen3(*cmd, &block)
    IO::popen3(*cmd, &block)
  end
  module_function :popen3

end
