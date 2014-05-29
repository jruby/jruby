describe :file_unlink, :shared => true do
  before :each do
    @file1 = 'test.txt'
    @file2 = 'test2.txt'

    touch @file1
    touch @file2
  end

  after :each do
    File.send(@method, @file1) if File.exists?(@file1)
    File.send(@method, @file2) if File.exists?(@file2)

    @file1 = nil
    @file2 = nil
  end

  it "returns 0 when called without arguments" do
    File.send(@method).should == 0
  end

  it "deletes a single file" do
    File.send(@method, @file1).should == 1
    File.exists?(@file1).should == false
  end

  it "deletes multiple files" do
    File.send(@method, @file1, @file2).should == 2
    File.exists?(@file1).should == false
    File.exists?(@file2).should == false
  end

  it "raises an TypeError if not passed a String type" do
    lambda { File.send(@method, 1) }.should raise_error(TypeError)
  end

  it "raises an Errno::ENOENT when the given file doesn't exist" do
    lambda { File.send(@method, 'bogus') }.should raise_error(Errno::ENOENT)
  end

  it "coerces a given parameter into a string if possible" do
    class Coercable
      def to_str
        "test.txt"
      end
    end

    File.send(@method, Coercable.new).should == 1
  end

  ruby_version_is "1.9" do
    it "accepts an object that has a #to_path method" do
      File.send(@method, mock_to_path(@file1)).should == 1
    end
  end
end
