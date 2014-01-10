require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/gets', __FILE__)

describe "ARGF.gets" do
  it_behaves_like :argf_gets, :gets
end

describe "ARGF.gets" do
  it_behaves_like :argf_gets_inplace_edit, :gets
end

describe "ARGF.gets" do
  before :each do
    @file1_name = fixture __FILE__, "file1.txt"
    @file2_name = fixture __FILE__, "file2.txt"

    @file1 = File.readlines @file1_name
    @file2 = File.readlines @file2_name
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "returns nil when reaching end of files" do
    argv [@file1_name, @file2_name] do
      total = @file1.size + @file2.size
      total.times { ARGF.gets }
      ARGF.gets.should == nil
    end
  end
end
