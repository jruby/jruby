require File.expand_path('../../../spec_helper', __FILE__)

describe "File.atime" do
  before :each do
    @file = tmp('test.txt')
    touch @file
  end

  after :each do
    rm_r @file
  end

  it "returns the last access time for the named file as a Time object" do
    File.atime(@file)
    File.atime(@file).should be_kind_of(Time)
  end

  it "raises an Errno::ENOENT exception if the file is not found" do
    lambda { File.atime('a_fake_file') }.should raise_error(Errno::ENOENT)
  end

  ruby_version_is "1.9" do
    it "accepts an object that has a #to_path method" do
      File.atime(mock_to_path(@file))
    end
  end
end

describe "File#atime" do
  before :each do
    @name = File.expand_path(__FILE__)
    @file = File.open(@name)
  end

  after :each do
    @file.close rescue nil
  end

  it "returns the last access time to self" do
    @file.atime
    @file.atime.should be_kind_of(Time)
  end
end
