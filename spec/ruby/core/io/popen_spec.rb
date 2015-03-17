require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "IO.popen" do
  ruby_exe = RUBY_EXE.split

  after :each do
    @io.close if @io and !@io.closed?
  end

  it "returns an open IO" do
    @io = IO.popen("#{RUBY_EXE} -e exit", "r")
    @io.closed?.should be_false
  end

  it "reads a read-only pipe" do
    @io = IO.popen("#{RUBY_EXE} -e 'puts \"foo\"'", "r")
    @io.read.should == "foo\n"
  end

  it "raises IOError when writing a read-only pipe" do
    @io = IO.popen("#{RUBY_EXE} -e 'puts \"foo\"'", "r")
    lambda { @io.write('foo') }.should raise_error(IOError)
  end

  before :each do
    @fname = tmp("IO_popen_spec")
  end

  after :each do
    rm_r @fname
  end

  it "writes to a write-only pipe" do
    @io = IO.popen("#{RUBY_EXE} -e 'IO.copy_stream(STDIN,STDOUT)' > #{@fname}", "w")
    @io.write("bar")
    @io.close

    @fname.should have_data("bar")
  end

  it "raises IOError when reading a write-only pipe" do
    @io = IO.popen("#{RUBY_EXE} -e 'IO.copy_stream(STDIN,STDOUT)'", "w")
    lambda { @io.read }.should raise_error(IOError)
  end

  it "reads and writes a read/write pipe" do
    @io = IO.popen("#{RUBY_EXE} -e 'IO.copy_stream(STDIN,STDOUT)'", "r+")
    @io.write("bar")
    @io.read(3).should == "bar"
  end

  it "waits for the child to finish" do
    @io = IO.popen("#{RUBY_EXE} -e 'IO.copy_stream(STDIN,STDOUT)' > #{@fname}", "w")
    @io.write("bar")
    @io.close

    $?.exitstatus.should == 0

    @fname.should have_data("bar")
  end

  it "does not throw an exception if child exited and has been waited for" do
    @io = IO.popen("#{RUBY_EXE} -e sleep")
    Process.kill "KILL", @io.pid
    @io.close
    $?.exitstatus.should be_nil
  end

  it "returns an instance of a subclass when called on a subclass" do
    @io = IOSpecs::SubIO.popen("#{RUBY_EXE} -e exit", "r")
    @io.should be_an_instance_of(IOSpecs::SubIO)
  end

  it "coerces mode argument with #to_str" do
    mode = mock("mode")
    mode.should_receive(:to_str).and_return("r")
    @io = IO.popen("true", mode)
  end

  describe "with a block" do
    it "yields an open IO to the block" do
      @io = IO.popen("#{RUBY_EXE} -e exit", "r") do |io|
        io.closed?.should be_false
      end
    end

    it "yields an instance of a subclass when called on a subclass" do
      IOSpecs::SubIO.popen("#{RUBY_EXE} -e exit", "r") do |io|
        io.should be_an_instance_of(IOSpecs::SubIO)
      end
    end

    it "closes the IO after yielding" do
      @io = IO.popen("#{RUBY_EXE} -e exit", "r") { |io| io }
      @io.closed?.should be_true
    end

    it "allows the IO to be closed inside the block" do
      @io = IO.popen("#{RUBY_EXE} -e exit", 'r') { |io| io.close; io }
      @io.closed?.should be_true
    end

    it "returns the value of the block" do
      IO.popen("#{RUBY_EXE} -e exit", "r") { :hello }.should == :hello
    end
  end

  with_feature :fork do
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
  end

  with_feature :encoding do
    it "has the given external encoding" do
      io = IO.popen("#{RUBY_EXE} -e exit", :external_encoding => Encoding::EUC_JP)
      io.external_encoding.should == Encoding::EUC_JP
    end

    it "has the given internal encoding" do
      io = IO.popen("#{RUBY_EXE} -e exit", :internal_encoding => Encoding::EUC_JP)
      io.internal_encoding.should == Encoding::EUC_JP
    end

    it "sets the internal encoding to nil if it's the same as the external encoding" do
      io = IO.popen("#{RUBY_EXE} -e exit", :external_encoding => Encoding::EUC_JP,
                            :internal_encoding => Encoding::EUC_JP)
      io.internal_encoding.should be_nil
    end
  end

  context "with a leading ENV Hash" do
    it "accepts a single String command" do
      IO.popen({"FOO" => "bar"}, "#{RUBY_EXE} -e 'puts ENV[\"FOO\"]'") do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts a single String command, and an IO mode" do
      IO.popen({"FOO" => "bar"}, "#{RUBY_EXE} -e 'puts ENV[\"FOO\"]'", "r") do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts a single String command with a trailing Hash of Process.exec options" do
      IO.popen({"FOO" => "bar"}, "#{RUBY_EXE} -e 'STDERR.puts ENV[\"FOO\"]'",
               :err => [:child, :out]) do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts a single String command with a trailing Hash of Process.exec options, and an IO mode" do
      IO.popen({"FOO" => "bar"}, "#{RUBY_EXE} -e 'STDERR.puts ENV[\"FOO\"]'", "r",
               :err => [:child, :out]) do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts an Array of command and arguments" do
      exe, *args = *ruby_exe
      IO.popen({"FOO" => "bar"}, [[exe, "specfu"], *args, "-e", "puts ENV['FOO']"]) do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts an Array of command and arguments, and an IO mode" do
      exe, *args = *ruby_exe
      IO.popen({"FOO" => "bar"}, [[exe, "specfu"], *args, "-e", "puts ENV['FOO']"], "r") do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts an Array command with a separate trailing Hash of Process.exec options" do |io|
      IO.popen({"FOO" => "bar"}, [*ruby_exe, "-e", "STDERR.puts ENV['FOO']"],
               :err => [:child, :out]) do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts an Array command with a separate trailing Hash of Process.exec options, and an IO mode" do |io|
      IO.popen({"FOO" => "bar"}, [*ruby_exe, "-e", "STDERR.puts ENV['FOO']"],
               "r", :err => [:child, :out]) do |io|
        io.read.should == "bar\n"
      end
    end
  end

  context "with a leading Array argument" do
    ruby_exe = RUBY_EXE.split

    it "uses the Array as command plus args for the child process" do
      IO.popen([*ruby_exe, "-e", "puts 'hello'"]) do |io|
        io.read.should == "hello\n"
      end
    end

    it "accepts a leading ENV Hash" do
      IO.popen([{"FOO" => "bar"}, *ruby_exe, "-e", "puts ENV['FOO']"]) do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts a trailing Hash of Process.exec options" do
      IO.popen([*ruby_exe, "does_not_exist", {:err => [:child, :out]}]) do |io|
        io.read.should =~ /LoadError/
      end
    end

    it "accepts an IO mode argument following the Array" do
      IO.popen([*ruby_exe, "does_not_exist", {:err => [:child, :out]}], "r") do |io|
        io.read.should =~ /LoadError/
      end
    end

    it "accepts [env, command, arg1, arg2, ..., exec options]" do |io|
      IO.popen([{"FOO" => "bar"}, *ruby_exe, "-e", "STDERR.puts ENV[:FOO.to_s]",
                :err => [:child, :out]]) do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts '[env, command, arg1, arg2, ..., exec options], mode'" do |io|
      IO.popen([{"FOO" => "bar"}, *ruby_exe, "-e", "STDERR.puts ENV[:FOO.to_s]",
                :err => [:child, :out]], "r") do |io|
        io.read.should == "bar\n"
      end
    end

    it "accepts '[env, command, arg1, arg2, ..., exec options], mode, IO options'" do |io|
      IO.popen([{"FOO" => "bar"}, *ruby_exe, "-e", "STDERR.puts ENV[:FOO.to_s]",
                :err => [:child, :out]], "r",
               :internal_encoding => Encoding::EUC_JP) do |io|
        io.read.should == "bar\n"
        io.internal_encoding.should == Encoding::EUC_JP
      end
    end

    it "accepts '[env, command, arg1, arg2, ...], mode, IO + exec options'" do |io|
      IO.popen([{"FOO" => "bar"}, *ruby_exe, "-e", "STDERR.puts ENV[:FOO.to_s]"], "r",
               :err => [:child, :out], :internal_encoding => Encoding::EUC_JP) do |io|
        io.read.should == "bar\n"
        io.internal_encoding.should == Encoding::EUC_JP
      end
    end
  end
end
