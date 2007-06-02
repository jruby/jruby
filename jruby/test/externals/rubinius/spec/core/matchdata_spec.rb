require File.dirname(__FILE__) + '/../spec_helper'

# [], begin, captures, end, inspect, length, offset, post_match, pre_match
# select, size, string, to_a, to_s

context "MatchData instance methods" do
  specify "[] should act as normal array indexing [index]" do
    /(.)(.)(\d+)(\d)/.match("THX1138.")[0].should == 'HX1138'
    /(.)(.)(\d+)(\d)/.match("THX1138.")[1].should == 'H'
    /(.)(.)(\d+)(\d)/.match("THX1138.")[2].should == 'X'
  end

  specify "[] should support accessors [start, length]" do
    /(.)(.)(\d+)(\d)/.match("THX1138.")[1, 2].should == %w|H X|
    /(.)(.)(\d+)(\d)/.match("THX1138.")[-3, 2].should == %w|X 113|
  end

  specify "[] should support ranges [start..end]" do
    /(.)(.)(\d+)(\d)/.match("THX1138.")[1..3].should == %w|H X 113|
  end

  specify "begin(index) should return the offset of the start of the nth element" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").begin(0).should == 1
    /(.)(.)(\d+)(\d)/.match("THX1138.").begin(2).should == 2
  end

  specify "captures should return and array of the match captures" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").captures.should == ["H","X","113","8"]
  end

  specify "end(index) should return the offset of the end of the nth element" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").end(0).should == 7
    /(.)(.)(\d+)(\d)/.match("THX1138.").end(2).should == 3 
  end

  specify "length should return the number of elements in the match array" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").length.should == 5
  end

  specify "size should return the number of elements in the match array" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").size.should == 5
  end

  specify "to_s should return the entire matched string" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").to_s.should == "HX1138"
  end

  specify "offset(index) should return a two element array with the begin and end of the nth match" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").offset(0).should == [1, 7]
    /(.)(.)(\d+)(\d)/.match("THX1138.").offset(4).should == [6, 7]
  end

  specify "post_match should return the string after the match equiv. special var $'" do
    /(.)(.)(\d+)(\d)/.match("THX1138: The Movie").post_match.should == ': The Movie' 
    $'.should == ': The Movie'
  end

  specify "pre_match should return the string before the match, equiv. special var $`" do
    /(.)(.)(\d+)(\d)/.match("THX1138: The Movie").pre_match.should == 'T'
    $`.should == 'T'
  end

  specify "values_at([index]*) should return an array of the matching value" do
    /(.)(.)(\d+)(\d)/.match("THX1138: The Movie").values_at(0, 2, -2).should == ["HX1138", "X", "113"]
  end

  specify "select (depreciated) should yield the contents of the match array to a block" do
     /(.)(.)(\d+)(\d)/.match("THX1138: The Movie").select { |x| x }.should == ["HX1138", "H", "X", "113", "8"]
  end

  specify "string should return a frozen copy of the match string" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").string.should == "THX1138."
  end

  specify "to_a returns an array of matches" do
    /(.)(.)(\d+)(\d)/.match("THX1138.").to_a.should == ["HX1138", "H", "X", "113", "8"]
  end
end

