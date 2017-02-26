require 'spec_helper'
require 'yaml'
require 'mspec/commands/mspec'

describe MSpecMain, "#options" do
  before :each do
    @options, @config = new_option
    MSpecOptions.stub(:new).and_return(@options)

    @script = MSpecMain.new
    @script.stub(:config).and_return(@config)
    @script.stub(:load)
  end

  it "enables the configure option" do
    @options.should_receive(:configure)
    @script.options
  end

  it "provides a custom action (block) to the config option" do
    @script.options ["-B", "config"]
    @config[:options].should include("-B", "config")
  end

  it "loads the file specified by the config option" do
    @script.should_receive(:load).with("config")
    @script.options ["-B", "config"]
  end

  it "enables the target options" do
    @options.should_receive(:targets)
    @script.options
  end

  it "sets config[:options] to all argv entries that are not registered options" do
    @options.on "-X", "--exclude", "ARG", "description"
    @script.options [".", "-G", "fail", "-X", "ARG", "--list", "unstable", "some/file.rb"]
    @config[:options].should == [".", "-G", "fail", "--list", "unstable", "some/file.rb"]
  end

  it "calls #custom_options" do
    @script.should_receive(:custom_options).with(@options)
    @script.options
  end
end

describe MSpecMain, "#parallel" do
  before :all do
    @verbose, $VERBOSE = $VERBOSE, nil
  end

  after :all do
    $VERBOSE = @verbose
  end

  before :each do
    @script = MSpecMain.new
    @ruby_platform = Object.const_get :RUBY_PLATFORM
  end

  after :each do
    Object.const_set :RUBY_PLATFORM, @ruby_platform
  end

  it "returns false if JRUBY_VERSION is defined" do
    Object.should_receive(:const_defined?).with(:JRUBY_VERSION).and_return(true)
    @script.parallel.should == false
  end

  it "returns false if RUBY_PLATFORM matches mswin" do
    Object.const_set :RUBY_PLATFORM, "i386-mswin32"
    @script.parallel.should == false
  end

  it "returns false if RUBY_PLATFORM matches mingw" do
    Object.const_set :RUBY_PLATFORM, "i386-mingw32"
    @script.parallel.should == false
  end

  it "returns true unless JRUBY_VERSION is set or RUBY_PLATFORM matches mswin or mingw" do
    Object.should_receive(:const_defined?).with(:JRUBY_VERSION).and_return(false)
    Object.const_set :RUBY_PLATFORM, "i686-linux"
    @script.parallel.should == true
  end
end

describe MSpecMain, "#fork" do
  before :each do
    @script = MSpecMain.new
    ScratchPad.clear
  end

  it "calls Kernel.fork if #parallel returns true" do
    @script.should_receive(:parallel).and_return(true)
    Kernel.should_receive(:fork)
    @script.fork
  end

  it "calls the block if #parallel returns false" do
    @script.should_receive(:parallel).and_return(false)
    Kernel.should_not_receive(:fork)
    @script.fork { ScratchPad.record :called }
    ScratchPad.recorded.should == :called
  end
end

describe MSpecMain, "#report" do
  before :each do
    @stdout, $stdout = $stdout, IOStub.new

    @timer = double("timer").as_null_object
    @timer.stub(:format).and_return("Finished in 42 seconds")
    @file = double("file").as_null_object

    File.stub(:delete)
    YAML.stub(:load)

    @hash = { "files"=>1, "examples"=>1, "expectations"=>2, "failures"=>0, "errors"=>0 }
    File.stub(:open).and_yield(@file).and_return(@hash)

    @script = MSpecMain.new
  end

  after :each do
    $stdout = @stdout
  end

  it "calls YAML.load for each element in the passed array" do
    YAML.should_receive(:load).with(@file).twice
    @script.report(["a", "b"], @timer)
  end

  it "calls File.delete for each element in the passed array" do
    File.should_receive(:delete).with("a")
    File.should_receive(:delete).with("b")
    @script.report(["a", "b"], @timer)
  end

  it "outputs a summary without errors" do
    @script.report(["a", "b"], @timer)
    $stdout.should ==
%[

Finished in 42 seconds

2 files, 2 examples, 4 expectations, 0 failures, 0 errors, 0 tagged
]
  end

  it "outputs a summary with errors" do
    @hash["exceptions"] = [
      "Some#method works real good FAILED\nExpected real good\n to equal fail\n\nfoo.rb:1\nfoo.rb:2",
      "Some#method never fails ERROR\nExpected 5\n to equal 3\n\nfoo.rb:1\nfoo.rb:2"
    ]
    @script.report(["a"], @timer)
    $stdout.should ==
%[

1)
Some#method works real good FAILED
Expected real good
 to equal fail

foo.rb:1
foo.rb:2

2)
Some#method never fails ERROR
Expected 5
 to equal 3

foo.rb:1
foo.rb:2

Finished in 42 seconds

1 file, 1 example, 2 expectations, 0 failures, 0 errors, 0 tagged
]
  end
end

describe MSpecMain, "#multi_exec" do
  before :each do
    @options, @config = new_option
    MSpecOptions.stub(:new).and_return(@options)

    @config[:target] = "target"
    @config[:ci_files] = ["a", "b"]

    @script = MSpecMain.new
    @script.stub(:config).and_return(@config)
    @script.stub(:fork)
    @script.stub(:report)
    Process.stub(:waitall)
  end

  it "calls #fork for each entry in config[:ci_files]" do
    @script.should_receive(:fork).twice
    @script.multi_exec []
  end

  it "calls Process.waitall" do
    Process.should_receive(:waitall)
    @script.multi_exec []
  end

  it "calls #report" do
    @script.should_receive(:report)
    @script.multi_exec []
  end
end

describe MSpecMain, "#run" do
  before :each do
    @options, @config = new_option
    MSpecOptions.stub(:new).and_return(@options)
    @script = MSpecMain.new
    @script.stub(:config).and_return(@config)
    @script.stub(:exec)
    @err = $stderr
    $stderr = IOStub.new
  end

  after :each do
    $stderr = @err
  end

  it "uses exec to invoke the runner script" do
    @script.should_receive(:exec).with("ruby", "-v", %r"#{MSPEC_HOME}/bin/mspec-run$")
    @script.options []
    @script.run
  end

  it "shows the command line on stderr" do
    @script.should_receive(:exec).with("ruby", "-v", %r"#{MSPEC_HOME}/bin/mspec-run$")
    @script.options []
    @script.run
    $stderr.to_s.should == "$ ruby -v #{Dir.pwd}/bin/mspec-run\n"
  end

  it "adds config[:launch] to the exec options" do
    @script.should_receive(:exec).with("ruby",
        "-Xlaunch.option", "-v", %r"#{MSPEC_HOME}/bin/mspec-run$")
    @config[:launch] << "-Xlaunch.option"
    @script.options []
    @script.run
    $stderr.to_s.should == "$ ruby -Xlaunch.option -v #{Dir.pwd}/bin/mspec-run\n"
  end

  it "calls #multi_exec if the command is 'ci' and the multi option is passed" do
    @script.should_receive(:multi_exec).and_return do |arg|
      arg.length.should == 3
      arg[0].should == "-v"
      arg[1].should =~ %r"#{MSPEC_HOME}/bin/mspec-ci$"
      arg[2].should == "-fy"
    end
    @script.options ["ci", "-j"]
    @script.run
  end
end

describe "The -A, --valgrind option" do
  before :each do
    @options, @config = new_option
    MSpecOptions.stub(:new).and_return(@options)
    @script = MSpecMain.new
    @script.stub(:config).and_return(@config)
  end

  it "is enabled by #options" do
    @options.stub(:on)
    @options.should_receive(:on).with("-A", "--valgrind", an_instance_of(String))
    @script.options
  end

  it "sets :use_valgrind config option to true" do
    ["-A", "--valgrind"].each do |opt|
      @config[:use_valgrind] = false
      @script.options [opt]
      @config[:use_valgrind].should be_true
    end
  end
end

describe "The --warnings option" do
  before :each do
    @options, @config = new_option
    MSpecOptions.stub(:new).and_return(@options)
    @script = MSpecMain.new
    @script.stub(:config).and_return(@config)
  end

  it "is enabled by #options" do
    @options.stub(:on)
    @options.should_receive(:on).with("--warnings", an_instance_of(String))
    @script.options
  end

  it "sets flags to -w" do
    @config[:flags] = []
    @script.options ["--warnings"]
    @config[:flags].should include("-w")
  end

  it "set OUTPUT_WARNINGS = '1' in the environment" do
    ENV['OUTPUT_WARNINGS'] = '0'
    @script.options ["--warnings"]
    ENV['OUTPUT_WARNINGS'].should == '1'
  end
end

describe "The -j, --multi option" do
  before :each do
    @options, @config = new_option
    MSpecOptions.stub(:new).and_return(@options)
    @script = MSpecMain.new
    @script.stub(:config).and_return(@config)
  end

  it "is enabled by #options" do
    @options.stub(:on)
    @options.should_receive(:on).with("-j", "--multi", an_instance_of(String))
    @script.options
  end

  it "sets the multiple process option" do
    ["-j", "--multi"].each do |opt|
      @config[:multi] = nil
      @script.options [opt]
      @config[:multi].should == true
    end
  end

  it "sets the formatter to YamlFormatter" do
    ["-j", "--multi"].each do |opt|
      @config[:options] = []
      @script.options [opt]
      @config[:options].should include("-fy")
    end
  end
end

describe "The -h, --help option" do
  before :each do
    @options, @config = new_option
    MSpecOptions.stub(:new).and_return(@options)
    @script = MSpecMain.new
    @script.stub(:config).and_return(@config)
  end

  it "is enabled by #options" do
    @options.stub(:on)
    @options.should_receive(:on).with("-h", "--help", an_instance_of(String))
    @script.options
  end

  it "passes the option to the subscript" do
    ["-h", "--help"].each do |opt|
      @config[:options] = []
      @script.options ["ci", opt]
      @config[:options].sort.should == ["-h"]
    end
  end

  it "prints help and exits" do
    @script.should_receive(:puts).twice
    @script.should_receive(:exit).twice
    ["-h", "--help"].each do |opt|
      @script.options [opt]
    end
  end
end

describe "The -v, --version option" do
  before :each do
    @options, @config = new_option
    MSpecOptions.stub(:new).and_return(@options)
    @script = MSpecMain.new
    @script.stub(:config).and_return(@config)
  end

  it "is enabled by #options" do
    @options.stub(:on)
    @options.should_receive(:on).with("-v", "--version", an_instance_of(String))
    @script.options
  end

  it "passes the option to the subscripts" do
    ["-v", "--version"].each do |opt|
      @config[:options] = []
      @script.options ["ci", opt]
      @config[:options].sort.should == ["-v"]
    end
  end

  it "prints the version and exits if no subscript is invoked" do
    @config[:command] = nil
    File.stub(:basename).and_return("mspec")
    @script.should_receive(:puts).twice.with("mspec #{MSpec::VERSION}")
    @script.should_receive(:exit).twice
    ["-v", "--version"].each do |opt|
      @script.options [opt]
    end
  end
end
