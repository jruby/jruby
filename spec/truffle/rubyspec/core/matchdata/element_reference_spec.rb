require File.expand_path('../../../spec_helper', __FILE__)

describe "MatchData#[]" do
  it "acts as normal array indexing [index]" do
    /(.)(.)(\d+)(\d)/.match("THX1138.")[0].should == 'HX1138'
    /(.)(.)(\d+)(\d)/.match("THX1138.")[1].should == 'H'
    /(.)(.)(\d+)(\d)/.match("THX1138.")[2].should == 'X'
  end

  it "supports accessors [start, length]" do
    /(.)(.)(\d+)(\d)/.match("THX1138.")[1, 2].should == %w|H X|
    /(.)(.)(\d+)(\d)/.match("THX1138.")[-3, 2].should == %w|X 113|
  end

  it "supports ranges [start..end]" do
    /(.)(.)(\d+)(\d)/.match("THX1138.")[1..3].should == %w|H X 113|
  end
end

language_version __FILE__, "element_reference"
