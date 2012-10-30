require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/open', __FILE__)

describe "File.open" do
  before :all do
    @file = tmp("test.txt")
    @nonexistent = tmp("fake.txt")
    rm_r @file, @nonexistent
  end

  before :each do
    ScratchPad.record []

    @fh = @fd = nil
    @flags = File::CREAT | File::TRUNC | File::WRONLY
    touch @file
  end

  after :each do
    @fh.close if @fh and not @fh.closed?
    rm_r @file, @nonexistent
  end

  describe "with a block" do
    it "does not raise error when file is closed inside the block" do
      @fh = File.open(@file) { |fh| fh.close; fh }
      @fh.closed?.should == true
    end

    it "invokes close on an opened file when exiting the block" do
      File.open(@file, 'r') { |f| FileSpecs.make_closer f }

      ScratchPad.recorded.should == [:file_opened, :file_closed]
    end

    it "propagates non-StandardErrors produced by close" do
      lambda {
        File.open(@file, 'r') { |f| FileSpecs.make_closer f, Exception }
      }.should raise_error(Exception)

      ScratchPad.recorded.should == [:file_opened, :file_closed]
    end

    it "does not propagate StandardErrors produced by close" do
      File.open(@file, 'r') { |f| FileSpecs.make_closer f, IOError }

      ScratchPad.recorded.should == [:file_opened, :file_closed]
    end
  end

  it "opens the file (basic case)" do
    @fh = File.open(@file)
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens file when call with a block (basic case)" do
    File.open(@file){ |fh| @fd = fh.fileno }
    lambda { File.open(@fd) }.should raise_error(SystemCallError) # Should be closed by block
    File.exist?(@file).should == true
  end

  it "opens with mode string" do
    @fh = File.open(@file, 'w')
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens a file with mode string and block" do
    File.open(@file, 'w'){ |fh| @fd = fh.fileno }
    lambda { File.open(@fd) }.should raise_error(SystemCallError)
    File.exist?(@file).should == true
  end

  it "opens a file with mode num" do
    @fh = File.open(@file, @flags)
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens a file with mode num and block" do
    File.open(@file, 'w'){ |fh| @fd = fh.fileno }
    lambda { File.open(@fd) }.should raise_error(SystemCallError)
    File.exist?(@file).should == true
  end

  it "opens a file with mode and permission as nil" do
    @fh = File.open(@file, nil, nil)
    @fh.should be_kind_of(File)
  end

  # For this test we delete the file first to reset the perms
  it "opens the file when passed mode, num and permissions" do
    rm_r @file
    File.umask(0011)
    @fh = File.open(@file, @flags, 0755)
    @fh.should be_kind_of(File)
    platform_is_not :windows do
      @fh.lstat.mode.to_s(8).should == "100744"
    end
    File.exist?(@file).should == true
  end

  # For this test we delete the file first to reset the perms
  it "opens the file when passed mode, num, permissions and block" do
    rm_r @file
    File.umask(0022)
    File.open(@file, "w", 0755){ |fh| @fd = fh.fileno }
    lambda { File.open(@fd) }.should raise_error(SystemCallError)
    platform_is_not :windows do
      File.stat(@file).mode.to_s(8).should == "100755"
    end
    File.exist?(@file).should == true
  end

  it "creates the file and returns writable descriptor when called with 'w' mode and r-o permissions" do
    # it should be possible to write to such a file via returned descriptior,
    # even though the file permissions are r-r-r.

    File.open(@file, "w", 0444) { |f| f.puts("test") }
    @file.should have_data("test\n")
  end

  it "opens the existing file, does not change permissions even when they are specified" do
    File.chmod(0664, @file)
    orig_perms = File.stat(@file).mode.to_s(8)
    File.open(@file, "w", 0444) { |f| f.puts("test") }

    File.stat(@file).mode.to_s(8).should == orig_perms
    @file.should have_data("test\n")
  end

  platform_is_not :windows do
    it "creates a new write-only file when invoked with 'w' and '0222'" do
      rm_r @file
      File.open(@file, 'w', 0222) {}
      File.readable?(@file).should == false
      File.writable?(@file).should == true
    end
  end

  it "opens the file when call with fd" do
    # store in an ivar so it doesn't GC before we go to close it in 'after'
    @fh_orig = File.open(@file)
    @fh = File.open(@fh_orig.fileno)
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens a file with a file descriptor d and a block" do
    @fh = File.open(@file)
    @fh.should be_kind_of(File)
    File.open(@fh.fileno) { |fh| @fd = fh.fileno; @fh.close }
    lambda { File.open(@fd) }.should raise_error(SystemCallError)
    File.exist?(@file).should == true
  end

  it "opens a file that no exists when use File::WRONLY mode" do
    lambda { File.open(@nonexistent, File::WRONLY) }.should raise_error(Errno::ENOENT)
  end

  it "opens a file that no exists when use File::RDONLY mode" do
    lambda { File.open(@nonexistent, File::RDONLY) }.should raise_error(Errno::ENOENT)
  end

  it "opens a file that no exists when use 'r' mode" do
    lambda { File.open(@nonexistent, 'r') }.should raise_error(Errno::ENOENT)
  end

  it "opens a file that no exists when use File::EXCL mode" do
    lambda { File.open(@nonexistent, File::EXCL) }.should raise_error(Errno::ENOENT)
  end

  it "opens a file that no exists when use File::NONBLOCK mode" do
    lambda { File.open(@nonexistent, File::NONBLOCK) }.should raise_error(Errno::ENOENT)
  end

  platform_is_not :openbsd do
    it "opens a file that no exists when use File::TRUNC mode" do
      lambda { File.open(@nonexistent, File::TRUNC) }.should raise_error(Errno::ENOENT)
    end
  end

  platform_is :openbsd do
    it "opens a file that no exists when use File::TRUNC mode" do
      lambda { File.open(@nonexistent, File::TRUNC) }.should raise_error(Errno::EINVAL)
    end
  end

  it "opens a file that no exists when use File::NOCTTY mode" do
    lambda { File.open(@nonexistent, File::NOCTTY) }.should raise_error(Errno::ENOENT)
  end

  it "opens a file that no exists when use File::CREAT mode" do
    @fh = File.open(@nonexistent, File::CREAT) { |f| f }
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens a file that no exists when use 'a' mode" do
    @fh = File.open(@nonexistent, 'a') { |f| f }
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens a file that no exists when use 'w' mode" do
    @fh = File.open(@nonexistent, 'w') { |f| f }
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  # Check the grants associated to the differents open modes combinations.
  it "raises an ArgumentError exception when call with an unknown mode" do
    lambda { File.open(@file, "q") }.should raise_error(ArgumentError)
  end

  it "can read in a block when call open with RDONLY mode" do
    File.open(@file, File::RDONLY) do |f|
      f.gets.should == nil
    end
  end

  it "can read in a block when call open with 'r' mode" do
    File.open(@file, "r") do |f|
      f.gets.should == nil
    end
  end

  it "raises an IO exception when write in a block opened with RDONLY mode" do
    File.open(@file, File::RDONLY) do |f|
      lambda { f.puts "writing ..." }.should raise_error(IOError)
    end
  end

  it "raises an IO exception when write in a block opened with 'r' mode" do
    File.open(@file, "r") do |f|
      lambda { f.puts "writing ..." }.should raise_error(IOError)
    end
  end

  it "can't write in a block when call open with File::WRONLY||File::RDONLY mode" do
    File.open(@file, File::WRONLY|File::RDONLY ) do |f|
      f.puts("writing").should == nil
    end
  end

  it "can't read in a block when call open with File::WRONLY||File::RDONLY mode" do
    lambda {
      File.open(@file, File::WRONLY|File::RDONLY ) do |f|
        f.gets.should == nil
      end
    }.should raise_error(IOError)
  end

  it "can write in a block when call open with WRONLY mode" do
    File.open(@file, File::WRONLY) do |f|
      f.puts("writing").should == nil
    end
  end

  it "can write in a block when call open with 'w' mode" do
    File.open(@file, "w") do |f|
      f.puts("writing").should == nil
    end
  end

  it "raises an IOError when read in a block opened with WRONLY mode" do
    File.open(@file, File::WRONLY) do |f|
      lambda { f.gets  }.should raise_error(IOError)
    end
  end

  it "raises an IOError when read in a block opened with 'w' mode" do
    File.open(@file, "w") do |f|
      lambda { f.gets   }.should raise_error(IOError)
    end
  end

  it "raises an IOError when read in a block opened with 'a' mode" do
    File.open(@file, "a") do |f|
      lambda { f.gets  }.should raise_error(IOError)
    end
  end

  it "raises an IOError when read in a block opened with 'a' mode" do
    File.open(@file, "a") do |f|
      f.puts("writing").should == nil
      lambda { f.gets }.should raise_error(IOError)
    end
  end

  it "raises an IOError when read in a block opened with 'a' mode" do
    File.open(@file, File::WRONLY|File::APPEND ) do |f|
      lambda { f.gets }.should raise_error(IOError)
    end
  end

  it "raises an IOError when read in a block opened with File::WRONLY|File::APPEND mode" do
    File.open(@file, File::WRONLY|File::APPEND ) do |f|
      f.puts("writing").should == nil
      lambda { f.gets }.should raise_error(IOError)
    end
  end

  ruby_bug "#", "1.8.7.299" do
    it "raises an IOError when read in a block opened with File::RDONLY|File::APPEND mode" do
      lambda {
        File.open(@file, File::RDONLY|File::APPEND ) do |f|
          f.puts("writing")
        end
      }.should raise_error(IOError)
    end
  end

  it "can read and write in a block when call open with RDWR mode" do
    File.open(@file, File::RDWR) do |f|
      f.gets.should == nil
      f.puts("writing").should == nil
      f.rewind
      f.gets.should == "writing\n"
    end
  end

  it "can't read in a block when call open with File::EXCL mode" do
    lambda {
      File.open(@file, File::EXCL) do |f|
        f.puts("writing").should == nil
      end
    }.should raise_error(IOError)
  end

  it "can read in a block when call open with File::EXCL mode" do
    File.open(@file, File::EXCL) do |f|
      f.gets.should == nil
    end
  end

  it "can read and write in a block when call open with File::RDWR|File::EXCL mode" do
    File.open(@file, File::RDWR|File::EXCL) do |f|
      f.gets.should == nil
      f.puts("writing").should == nil
      f.rewind
      f.gets.should == "writing\n"
    end
  end

  it "raises an Errorno::EEXIST if the file exists when open with File::CREAT|File::EXCL" do
    lambda {
      File.open(@file, File::CREAT|File::EXCL) do |f|
        f.puts("writing")
      end
    }.should raise_error(Errno::EEXIST)
  end

  it "create a new file when use File::WRONLY|File::APPEND mode" do
    @fh = File.open(@file, File::WRONLY|File::APPEND)
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens a file when use File::WRONLY|File::APPEND mode" do
    File.open(@file, File::WRONLY) do |f|
      f.puts("hello file")
    end
    File.open(@file, File::RDWR|File::APPEND) do |f|
      f.puts("bye file")
      f.rewind
      f.gets().should == "hello file\n"
      f.gets().should == "bye file\n"
      f.gets().should == nil
    end
  end

  ruby_bug "#", "1.8.7.299" do
    it "raises an IOError if the file exists when open with File::RDONLY|File::APPEND" do
      lambda {
        File.open(@file, File::RDONLY|File::APPEND) do |f|
          f.puts("writing").should == nil
        end
      }.should raise_error(IOError)
    end
  end

  platform_is_not :openbsd do

    it "truncates the file when passed File::TRUNC mode" do
      File.open(@file, File::RDWR) { |f| f.puts "hello file" }
      @fh = File.open(@file, File::TRUNC)
      @fh.gets.should == nil
    end

    it "can't read in a block when call open with File::TRUNC mode" do
      File.open(@file, File::TRUNC) do |f|
        f.gets.should == nil
      end
    end

  end

  it "opens a file when use File::WRONLY|File::TRUNC mode" do
    fh1 = File.open(@file, "w")
    begin
      @fh = File.open(@file, File::WRONLY|File::TRUNC)
      @fh.should be_kind_of(File)
      File.exist?(@file).should == true
    ensure
      fh1.close
    end
  end

  platform_is_not :openbsd do
    it "can't write in a block when call open with File::TRUNC mode" do
      lambda {
        File.open(@file, File::TRUNC) do |f|
          f.puts("writing")
        end
      }.should raise_error(IOError)
    end

    it "raises an Errorno::EEXIST if the file exists when open with File::RDONLY|File::TRUNC" do
      lambda {
        File.open(@file, File::RDONLY|File::TRUNC) do |f|
          f.puts("writing").should == nil
        end
      }.should raise_error(IOError)
    end
  end

  platform_is :openbsd do
    it "can't write in a block when call open with File::TRUNC mode" do
      lambda {
        File.open(@file, File::TRUNC) do |f|
          f.puts("writing")
        end
      }.should raise_error(Errno::EINVAL)
    end

    it "raises an Errorno::EEXIST if the file exists when open with File::RDONLY|File::TRUNC" do
      lambda {
        File.open(@file, File::RDONLY|File::TRUNC) do |f|
          f.puts("writing").should == nil
        end
      }.should raise_error(Errno::EINVAL)
    end
  end

  platform_is_not :windows do
    it "raises an Errno::EACCES when opening non-permitted file" do
      @fh = File.open(@file, "w")
      @fh.chmod(000)
      lambda { fh1 = File.open(@file); fh1.close }.should raise_error(Errno::EACCES)
    end
  end

  it "raises an Errno::EACCES when opening read-only file" do
    @fh = File.open(@file, "w")
    @fh.chmod(0444)
    lambda { File.open(@file, "w") }.should raise_error(Errno::EACCES)
  end

  it "opens a file for binary read" do
    @fh = File.open(@file, "rb")
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens a file for binary write" do
    @fh = File.open(@file, "wb")
    @fh.should be_kind_of(File)
    File.exist?(@file).should == true
  end

  it "opens a file for read-write and truncate the file" do
    File.open(@file, "w") { |f| f.puts "testing" }
    File.size(@file).should > 0
    File.open(@file, "w+") do |f|
      f.pos.should == 0
      f.eof?.should == true
    end
    File.size(@file).should == 0
  end

  it "opens a file for binary read-write starting at the beginning of the file" do
    File.open(@file, "w") { |f| f.puts "testing" }
    File.size(@file).should > 0
    File.open(@file, "rb+") do |f|
      f.pos.should == 0
      f.eof?.should == false
    end
  end

  it "opens a file for binary read-write and truncate the file" do
    File.open(@file, "w") { |f| f.puts "testing" }
    File.size(@file).should > 0
    File.open(@file, "wb+") do |f|
      f.pos.should == 0
      f.eof?.should == true
    end
    File.size(@file).should == 0
  end

  it "raises a TypeError if passed a filename that is not a String or Integer type" do
    lambda { File.open(true)  }.should raise_error(TypeError)
    lambda { File.open(false) }.should raise_error(TypeError)
    lambda { File.open(nil)   }.should raise_error(TypeError)
  end

  it "raises a SystemCallError if passed an invalid Integer type" do
    lambda { File.open(-1)    }.should raise_error(SystemCallError)
  end

  it "raises an ArgumentError if passed the wrong number of arguments" do
    lambda { File.open(@file, File::CREAT, 0755, 'test') }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if passed an invalid string for mode" do
    lambda { File.open(@file, 'fake') }.should raise_error(ArgumentError)
  end
  
  ruby_version_is "1.9.2" do
    it "defaults external_encoding to ASCII-8BIT for binary modes" do
      File.open(@file, 'rb') {|f| f.external_encoding.should == Encoding::ASCII_8BIT}
      File.open(@file, 'wb+') {|f| f.external_encoding.should == Encoding::ASCII_8BIT}
    end
  end

  ruby_version_is "1.9" do
    it "needs to be completed for hash argument"
  end
end

describe "File.open" do
  it_behaves_like :open_directory, :open
end
