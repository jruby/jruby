describe :language___FILE__, :shared => true do
  before :each do
    CodeLoadingSpecs.spec_setup
    if File.respond_to?(:realpath)
      @path = File.realpath("file_fixture.rb", CODE_LOADING_DIR)
    else
      @path = File.expand_path("file_fixture.rb", CODE_LOADING_DIR)
    end
  end

  after :each do
    CodeLoadingSpecs.spec_cleanup
  end

  it "equals the absolute path of a file loaded by an absolute path" do
    @object.send(@method, @path).should be_true
    ScratchPad.recorded.should == [@path]
  end

  it "equals the absolute path of a file loaded by a relative path" do
    $LOAD_PATH << "."
    Dir.chdir CODE_LOADING_DIR do
      @object.send(@method, "file_fixture.rb").should be_true
    end
    ScratchPad.recorded.should == [@path]
  end
end
