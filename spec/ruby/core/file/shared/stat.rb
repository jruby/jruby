describe :file_stat, :shared => true do
  before :each do
    @file = tmp('i_exist')
    touch(@file) { |f| f.write 'rubinius' }
  end

  after :each do
    rm_r @file
  end

  # TODO: Fix
  it "returns a File::Stat object if the given file exists" do
    st = File.send(@method, @file)

    st.file?.should == true
    st.zero?.should == false
    st.size.should == 8
    st.size?.should == 8
    st.blksize.should > 0
    st.atime.should be_kind_of(Time)
    st.ctime.should be_kind_of(Time)
    st.mtime.should be_kind_of(Time)
  end

  # TODO: Fix
  it "returns a File::Stat object when called on an instance of File" do
    File.open(@file) do |f|
      st = f.send(@method)

      st.file?.should == true
      st.zero?.should == false
      st.size.should == 8
      st.size?.should == 8
      st.blksize.should > 0
      st.atime.should be_kind_of(Time)
      st.ctime.should be_kind_of(Time)
      st.mtime.should be_kind_of(Time)
    end
  end

  ruby_version_is "1.9" do
    it "accepts an object that has a #to_path method" do
      File.send(@method, mock_to_path(@file))
    end
  end

  it "raises an Errno::ENOENT if the file does not exist" do
    lambda {
      File.send(@method, "fake_file")
    }.should raise_error(Errno::ENOENT)
  end
end
