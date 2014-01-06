describe :language___FILE__, :shared => true do
  before :each do
    CodeLoadingSpecs.spec_setup
    @path = File.expand_path("file_fixture.rb", CODE_LOADING_DIR)
  end

  after :each do
    CodeLoadingSpecs.spec_cleanup
  end

  it "equals the absolute path of a file loaded by an absolute path" do
    @object.send(@method, @path).should be_true
    ScratchPad.recorded.should == [@path]
  end

  ruby_version_is ""..."1.8.8" do
    it "equals the relative path of a file loaded by a relative path" do
      $LOAD_PATH << "."
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, "file_fixture.rb").should be_true
      end
      ScratchPad.recorded.should == ["./file_fixture.rb"]
    end
  end

  ruby_version_is "1.8.8" do
    it "equals the absolute path of a file loaded by a relative path" do
      $LOAD_PATH << "."
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, "file_fixture.rb").should be_true
      end
      ScratchPad.recorded.should == [@path]
    end
  end
end
