require File.dirname(__FILE__) + '/../spec_helper'

context "String instance method" do
  specify "prefix? should be true if string begins with argument" do
    "blah".prefix?("bl").should == true
    "blah".prefix?("fo").should == false
    "go".prefix?("gogo").should == false
  end  

  specify "substring should return the portion of string specified by index, length" do
    "blah".substring(0, 2).should == "bl"
    "blah".substring(0, 4).should == "blah"
    "blah".substring(2, 2).should == "ah"
  end
  
end

context "String implementation" do
  specify "underlying storage should have the correct size (space for last \0 and multiple of 4)" do
    "hell".data.size.should == 8
  end
end
