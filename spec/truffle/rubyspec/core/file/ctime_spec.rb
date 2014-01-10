require File.expand_path('../../../spec_helper', __FILE__)

describe "File.ctime" do
  before :each do
    @file = __FILE__
  end

  after :each do
    @file = nil
  end

  it "Returns the change time for the named file (the time at which directory information about the file was changed, not the file itself)." do
    File.ctime(@file)
    File.ctime(@file).should be_kind_of(Time)
  end

  ruby_version_is "1.9" do
    it "accepts an object that has a #to_path method" do
      File.ctime(mock_to_path(@file))
    end
  end

  it "raises an Errno::ENOENT exception if the file is not found" do
    lambda { File.ctime('bogus') }.should raise_error(Errno::ENOENT)
  end
end

describe "File#ctime" do
  before :each do
    @file = File.open(__FILE__)
  end

  after:each do
    @file.close
    @file = nil
  end

  it "Returns the change time for the named file (the time at which directory information about the file was changed, not the file itself)." do
    @file.ctime
    @file.ctime.should be_kind_of(Time)
  end
end
