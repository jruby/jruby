require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/file/file', __FILE__)

describe "File" do
  it "includes File::Constants" do
    File.include?(File::Constants).should == true
  end
end

describe "File.file?" do
  it_behaves_like :file_file, :file?, File
end
