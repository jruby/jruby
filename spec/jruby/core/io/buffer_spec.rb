require 'etc'
require 'rbconfig'

# The constants are read as the class loads, so their fallbacks and settings are checked in a JRuby of their own.
def io_buffer_constant(name, env: {}, options: [])
  IO.popen([env, RbConfig.ruby, *options, "-e", "print IO::Buffer::#{name}"], &:read).to_i
end

describe "IO::Buffer::PAGE_SIZE" do
  it "is the operating system's page size" do
    size = IO::Buffer::PAGE_SIZE

    size.should >= 4096
    (size & (size - 1)).should == 0
    if defined?(Etc::SC_PAGESIZE) && (expected = Etc.sysconf(Etc::SC_PAGESIZE))
      size.should == expected
    end
  end

  it "is 4096 without native access, where the operating system's can't be read" do
    io_buffer_constant(:PAGE_SIZE, options: ["-Xnative.enabled=false"]).should == 4096
  end
end

describe "IO::Buffer::DEFAULT_SIZE" do
  it "is 64 KiB, or the page size where that's larger" do
    skip "RUBY_IO_BUFFER_DEFAULT_SIZE is set" if ENV["RUBY_IO_BUFFER_DEFAULT_SIZE"]

    IO::Buffer::DEFAULT_SIZE.should == [64 * 1024, IO::Buffer::PAGE_SIZE].max
  end

  it "is RUBY_IO_BUFFER_DEFAULT_SIZE, where that's a positive number, read by its leading digits" do
    io_buffer_constant(:DEFAULT_SIZE, env: {"RUBY_IO_BUFFER_DEFAULT_SIZE" => "8192"}).should == 8192
    io_buffer_constant(:DEFAULT_SIZE, env: {"RUBY_IO_BUFFER_DEFAULT_SIZE" => " 128k"}).should == 128
  end

  it "ignores a RUBY_IO_BUFFER_DEFAULT_SIZE that isn't a positive number" do
    default = [64 * 1024, IO::Buffer::PAGE_SIZE].max

    io_buffer_constant(:DEFAULT_SIZE, env: {"RUBY_IO_BUFFER_DEFAULT_SIZE" => "-5"}).should == default
    io_buffer_constant(:DEFAULT_SIZE, env: {"RUBY_IO_BUFFER_DEFAULT_SIZE" => "large"}).should == default
  end
end
