require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "IO.popen" do
  after :each do
    @io.close if @io and !@io.closed?
  end

  it "returns an open IO" do
    @io = IO.popen("yes", "r")
    @io.closed?.should be_false
  end

  it "reads a read-only pipe" do
    @io = IO.popen("echo foo", "r")
    @io.read.should == "foo\n"
  end

  it "raises IOError when writing a read-only pipe" do
    @io = IO.popen("echo foo", "r")
    lambda { @io.write('foo') }.should raise_error(IOError)
  end

  platform_is_not :windows do
    before :each do
      @fname = tmp("IO_popen_spec")
    end

    after :each do
      rm_r @fname
    end

    it "writes to a write-only pipe" do
      @io = IO.popen("cat > #{@fname}", "w")
      @io.write("bar")
      @io.close

      @fname.should have_data("bar")
    end

    it "raises IOError when reading a write-only pipe" do
      @io = IO.popen("cat", "w")
      lambda { @io.read }.should raise_error(IOError)
    end

    it "reads and writes a read/write pipe" do
      @io = IO.popen("cat", "r+")
      @io.write("bar")
      @io.read(3).should == "bar"
    end

    it "waits for the child to finish" do
      @io = IO.popen("cat > #{@fname} && sleep 1", "w")
      @io.write("bar")
      @io.close

      $?.exitstatus.should == 0

      @fname.should have_data("bar")
    end

    it "does not throw an exception if child exited and has been waited for" do
      @io = IO.popen("echo ready; sleep 1000")
      true until @io.gets =~ /ready/
      Process.kill "KILL", @io.pid
      Process.wait @io.pid
      @io.close
      $?.exitstatus.should be_nil
    end
  end

  it "returns an instance of a subclass when called on a subclass" do
    @io = IOSpecs::SubIO.popen("true", "r")
    @io.should be_an_instance_of(IOSpecs::SubIO)
  end

  it "coerces mode argument with #to_str" do
    mode = mock("mode")
    mode.should_receive(:to_str).and_return("r")
    @io = IO.popen("true", mode)
  end

  describe "with a block" do
    it "yields an open IO to the block" do
      @io = IO.popen("yes", "r") do |io|
        io.closed?.should be_false
      end
    end

    it "yields an instance of a subclass when called on a subclass" do
      IOSpecs::SubIO.popen("true", "r") do |io|
        io.should be_an_instance_of(IOSpecs::SubIO)
      end
    end

    it "closes the IO after yielding" do
      @io = IO.popen("yes", "r") { |io| io }
      @io.closed?.should be_true
    end

    it "allows the IO to be closed inside the block" do
      @io = IO.popen('yes', 'r') { |io| io.close; io }
      @io.closed?.should be_true
    end

    it "returns the value of the block" do
      IO.popen("yes", "r") { :hello }.should == :hello
    end
  end

  it "starts returns a forked process if the command is -" do
    io = IO.popen("-")

    if io # parent
      io.gets.should == "hello from child\n"
      io.close
    else # child
      puts "hello from child"
      exit!
    end
  end

  ruby_version_is "1.9.2" do
    platform_is_not :windows do # not sure what commands to use on Windows
      describe "with a leading Array parameter" do
        it "uses the Array as command plus args for the child process" do
          IO.popen(["yes", "hello"]) do |i|
            i.read(5).should == 'hello'
          end
        end

        it "uses a leading Hash in the Array as additional environment variables" do
          IO.popen([{'foo' => 'bar'}, 'env']) do |i|
            i.read.should =~ /foo=bar/
          end
        end

        it "uses a trailing Hash in the Array for spawn-like settings" do
          IO.popen(['sh', '-c', 'does_not_exist', {:err => [:child, :out]}]) do |i|
            i.read.should =~ /not found/
          end
        end
      end
    end
  end
end
