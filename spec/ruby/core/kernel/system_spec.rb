require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe :kernel_system, shared: true do
  it "executes the specified command in a subprocess" do
    lambda { @object.system("echo a") }.should output_to_fd("a\n")
  end

  it "returns true when the command exits with a zero exit status" do
    @object.system("#{RUBY_EXE} -e 'exit 0'").should == true
  end

  it "returns false when the command exits with a non-zero exit status" do
    @object.system("#{RUBY_EXE} -e 'exit 1'").should == false
  end

  it "returns nil when command execution fails" do
    @object.system("sad").should be_nil
  end

  it "does not write to stderr when command execution fails" do
    lambda { @object.system("sad") }.should output_to_fd("", STDERR)
  end

  platform_is_not :windows do
    it "executes with `sh` if the command contains shell characters" do
      lambda { @object.system("echo $0") }.should output_to_fd("sh\n")
    end

    it "ignores SHELL env var and always uses `sh`" do
      lambda { @object.system("SHELL=/bin/zsh echo $0") }.should output_to_fd("sh\n")
    end
  end

  before :each do
    ENV['TEST_SH_EXPANSION'] = 'foo'
    @shell_var = '$TEST_SH_EXPANSION'
    platform_is :windows do
      @shell_var = '%TEST_SH_EXPANSION%'
    end
  end

  after :each do
    ENV.delete('TEST_SH_EXPANSION')
  end

  it "expands shell variables when given a single string argument" do
    lambda { @object.system("echo #{@shell_var}") }.should output_to_fd("foo\n")
  end

  platform_is_not :windows do
    it "does not expand shell variables when given multiples arguments" do
      lambda { @object.system("echo", @shell_var) }.should output_to_fd("#{@shell_var}\n")
    end
  end

  platform_is :windows do
    it "does expand shell variables when given multiples arguments" do
      # See https://bugs.ruby-lang.org/issues/12231
      lambda { @object.system("echo", @shell_var) }.should output_to_fd("foo\n")
    end
  end

  platform_is :windows do
    it "runs commands starting with any number of @ using shell" do
      `#{RUBY_EXE} -e "p system 'does_not_exist'" 2>NUL`.chomp.should == "nil"
      @object.system('@does_not_exist').should == false
      @object.system("@@@#{RUBY_EXE} -e 'exit 0'").should == true
    end
  end
end

describe "Kernel#system" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:system)
  end

  it_behaves_like :kernel_system, :system, KernelSpecs::Method.new
end

describe "Kernel.system" do
  it_behaves_like :kernel_system, :system, Kernel
end
