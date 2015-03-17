require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe Object, "#fixture" do
  before :each do
    @dir = File.expand_path(Dir.pwd)
  end

  it "returns the expanded path to a fixture file" do
    name = fixture("some/path/file.rb", "dir", "file.txt")
    name.should == "#{@dir}/some/path/fixtures/dir/file.txt"
  end

  it "omits '/shared' if it is the suffix of the directory string" do
    name = fixture("some/path/shared/file.rb", "dir", "file.txt")
    name.should == "#{@dir}/some/path/fixtures/dir/file.txt"
  end

  it "does not append '/fixtures' if it is the suffix of the directory string" do
    name = fixture("some/path/fixtures/file.rb", "dir", "file.txt")
    name.should == "#{@dir}/some/path/fixtures/dir/file.txt"
  end
end
