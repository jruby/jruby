require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

describe "Dir.entries" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it "returns an Array of filenames in an existing directory including dotfiles" do
    a = Dir.entries(DirSpecs.mock_dir).sort

    a.should == DirSpecs.expected_paths

    a = Dir.entries("#{DirSpecs.mock_dir}/deeply/nested").sort
    a.should == %w|. .. .dotfile.ext directory|
  end

  ruby_version_is "1.9" do
    it "calls #to_path on non-String arguments" do
      p = mock('path')
      p.should_receive(:to_path).and_return(DirSpecs.mock_dir)
      Dir.entries(p)
    end

    it "accepts an options Hash" do
      a = Dir.entries("#{DirSpecs.mock_dir}/deeply/nested", :encoding => "utf-8").sort
      a.should == %w|. .. .dotfile.ext directory|
    end
  end

  it "raises a SystemCallError if called with a nonexistent diretory" do
    lambda { Dir.entries DirSpecs.nonexistent }.should raise_error(SystemCallError)
  end
end
