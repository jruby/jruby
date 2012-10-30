describe :kernel_load, :shared => true do
  before :each do
    CodeLoadingSpecs.spec_setup
    @path = File.expand_path "load_fixture.rb", CODE_LOADING_DIR
  end

  after :each do
    CodeLoadingSpecs.spec_cleanup
  end

  it "loads a non-extensioned file as a Ruby source file" do
    path = File.expand_path "load_fixture", CODE_LOADING_DIR
    @object.load(path).should be_true
    ScratchPad.recorded.should == [:no_ext]
  end

  it "loads a non .rb extensioned file as a Ruby source file" do
    path = File.expand_path "load_fixture.ext", CODE_LOADING_DIR
    @object.load(path).should be_true
    ScratchPad.recorded.should == [:no_rb_ext]
  end

  ruby_version_is "1.9" do
    it "loads from the current working directory" do
      Dir.chdir CODE_LOADING_DIR do
        @object.load("load_fixture.rb").should be_true
        ScratchPad.recorded.should == [:loaded]
      end
    end
  end

  it "loads a file that recursively requires itself" do
    path = File.expand_path "recursive_require_fixture.rb", CODE_LOADING_DIR
    @object.load(path).should be_true
    ScratchPad.recorded.should == [:loaded, :loaded]
  end

  it "loads a file that recursively loads itself" do
    path = File.expand_path "recursive_load_fixture.rb", CODE_LOADING_DIR
    @object.load(path).should be_true
    ScratchPad.recorded.should == [:loaded, :loaded]
  end

  it "loads a file each time the method is called" do
    @object.load(@path).should be_true
    @object.load(@path).should be_true
    ScratchPad.recorded.should == [:loaded, :loaded]
  end

  it "loads a file even when the name appears in $LOADED_FEATURES" do
    $LOADED_FEATURES << @path
    @object.load(@path).should be_true
    ScratchPad.recorded.should == [:loaded]
  end

  it "loads a file that has been loaded by #require" do
    @object.require(@path).should be_true
    @object.load(@path).should be_true
    ScratchPad.recorded.should == [:loaded, :loaded]
  end

  it "does not cause #require with the same path to fail" do
    @object.load(@path).should be_true
    @object.require(@path).should be_true
    ScratchPad.recorded.should == [:loaded, :loaded]
  end

  it "does not add the loaded path to $LOADED_FEATURES" do
    @object.load(@path).should be_true
    $LOADED_FEATURES.should == []
  end

  it "raises a LoadError if passed a non-extensioned path that does not exist but a .rb extensioned path does exist" do
    path = File.expand_path "load_ext_fixture", CODE_LOADING_DIR
    lambda { @object.load(path) }.should raise_error(LoadError)
  end

  it "sets the enclosing scope to an anonymous module if passed true for 'wrap'" do
    path = File.expand_path "wrap_fixture.rb", CODE_LOADING_DIR
    @object.load(path, true).should be_true

    Object.const_defined?(:LoadSpecWrap).should be_false
    ScratchPad.recorded.first.should be_an_instance_of(Class)
  end

  describe "(shell expansion)" do
    before :all do
      @env_home = ENV["HOME"]
      ENV["HOME"] = CODE_LOADING_DIR
    end

    after :all do
      ENV["HOME"] = @env_home
    end

    it "expands a tilde to the HOME environment variable as the path to load" do
      @object.require("~/load_fixture.rb").should be_true
      ScratchPad.recorded.should == [:loaded]
    end
  end
end
