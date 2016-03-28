require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/write', __FILE__)

describe "IO#write_nonblock on a file" do
  before :each do
    @filename = tmp("IO_syswrite_file") + $$.to_s
    File.open(@filename, "w") do |file|
      file.write_nonblock("012345678901234567890123456789")
    end
    @file = File.open(@filename, "r+")
    @readonly_file = File.open(@filename)
  end

  after :each do
    @file.close
    @readonly_file.close
    rm_r @filename
  end

  it "writes all of the string's bytes but does not buffer them" do
    written = @file.write_nonblock("abcde")
    written.should == 5
    File.open(@filename) do |file|
      file.sysread(10).should == "abcde56789"
      file.seek(0)
      @file.fsync
      file.sysread(10).should == "abcde56789"
    end
  end

  it "checks if the file is writable if writing zero bytes" do
    lambda { @readonly_file.write_nonblock("") }.should raise_error
  end
end

describe 'IO#write_nonblock' do
  before do
    @read, @write = IO.pipe
  end

  after do
    @read.close
    @write.close
  end

  it "raises EAGAIN or a subclass when the write would block" do
    lambda {
      loop { @write.write_nonblock('a' * 10_000) }
    }.should raise_error(Errno::EAGAIN)
  end

  it "raises an exception extending IO::WaitWritable when the write would block" do
    lambda {
      loop { @write.write_nonblock('a' * 10_000) }
    }.should raise_error(IO::WaitWritable)
  end

  it 'raises IO::EAGAINWaitWritable when the operation would block' do
    lambda {
      loop { @write.write_nonblock('a' * 10_000) }
    }.should raise_error(IO::EAGAINWaitWritable)
  end

  ruby_version_is "2.3" do
    context "when exception option is set to false" do
      it "returns :wait_writable when the operation would block" do
        loop { break if @write.write_nonblock("a" * 10_000, exception: false) == :wait_writable }
        1.should == 1
      end
    end
  end

end

describe "IO#write_nonblock" do
  it_behaves_like :io_write, :write_nonblock
end
