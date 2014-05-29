describe :file_identical, :shared => true do
  before :each do
    @file1 = tmp('file_identical.txt')
    @file2 = tmp('file_identical2.txt')
    @link  = tmp('file_identical.lnk')
    @non_exist = 'non_exist'

    touch(@file1) { |f| f.puts "file1" }
    touch(@file2) { |f| f.puts "file2" }

    rm_r @link
    File.link(@file1, @link)
  end

  after :each do
    rm_r @link, @file1, @file2
  end

  it "returns true for a file and its link" do
    @object.send(@method, @file1, @link).should == true
  end

  it "returns false if any of the files doesn't exist" do
    @object.send(@method, @file1, @non_exist).should be_false
    @object.send(@method, @non_exist, @file1).should be_false
    @object.send(@method, @non_exist, @non_exist).should be_false
  end

  ruby_version_is ""..."1.9" do
    it "accepts an object that has a #to_str method" do
      file1, link = mock('file1'), mock('link')
      file1.should_receive(:to_str).and_return(@file1)
      link.should_receive(:to_str).and_return(@link)
      @object.send(@method, file1, link).should == true
    end
  end

  ruby_version_is "1.9" do
    it "accepts an object that has a #to_path method" do
      @object.send(@method, mock_to_path(@file1), mock_to_path(@link)).should == true
    end
  end

  it "raises an ArgumentError if not passed two arguments" do
    lambda { @object.send(@method, @file1, @file2, @link) }.should raise_error(ArgumentError)
    lambda { @object.send(@method, @file1) }.should raise_error(ArgumentError)
  end

  it "raises a TypeError if not passed String types" do
    lambda { @object.send(@method, 1,1) }.should raise_error(TypeError)
  end

  it "returns true if both named files are identical" do
    @object.send(@method, @file1, @file1).should be_true
    @object.send(@method, @link, @link).should be_true
    @object.send(@method, @file1, @file2).should be_false
  end
end
