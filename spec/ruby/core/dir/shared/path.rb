require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/common', __FILE__)
require File.expand_path('../closed', __FILE__)

describe :dir_path, :shared => true do
  it "returns the path that was supplied to .new or .open" do
    dir = Dir.open DirSpecs.mock_dir
    dir.send(@method).should == DirSpecs.mock_dir
    dir.close rescue nil
  end

  ruby_version_is ""..."1.8.7" do
    it "raises an IOError when called on a closed Dir instance" do
      lambda {
        dir = Dir.open DirSpecs.mock_dir
        dir.close
        dir.send(@method)
      }.should raise_error(IOError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns the path even when called on a closed Dir instance" do
      dir = Dir.open DirSpecs.mock_dir
      dir.close
      dir.send(@method).should == DirSpecs.mock_dir
    end
  end
end
