require File.expand_path('../../../spec_helper', __FILE__)

describe "File.readlink" do

  before :each do
    @file = tmp('file_readlink.txt')
    @link = tmp('file_readlink.lnk')

    touch @file

    rm_r @link
    File.symlink(@file, @link)
  end

  after :each do
    rm_r @link, @file
  end

  # symlink/readlink are not supported on Windows
  platform_is_not :windows do
    it "return the name of the file referenced by the given link" do
      File.readlink(@link).should == @file
    end

    it "raises an Errno::ENOENT if called with an invalid argument" do
      # TODO: missing_file
      lambda { File.readlink("/this/surely/doesnt/exist") }.should raise_error(Errno::ENOENT)
    end
  end
end
