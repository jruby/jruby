describe :file_sticky, :shared => true do
  before :each do
    @dir = tmp('sticky_dir')
    Dir.rmdir(@dir) if File.exists?(@dir)
  end

  after :each do
    Dir.rmdir(@dir) if File.exists?(@dir)
  end

  platform_is_not :windows, :darwin, :freebsd, :netbsd, :openbsd, :solaris do
    it "returns true if the named file has the sticky bit, otherwise false" do
      Dir.mkdir @dir, 1755

      @object.send(@method, @dir).should == true
      @object.send(@method, '/').should == false
    end
  end

  ruby_version_is "1.9" do
# please add a 1.9 test that accepts a mock_to_path("/path") argument
#    it "accepts an object that has a #to_path method"
  end
end

describe :file_sticky_missing, :shared => true do
  platform_is_not :windows do
    it "returns false if the file dies not exist" do
      @object.send(@method, 'fake_file').should == false
    end
  end
end
