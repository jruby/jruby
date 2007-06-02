require File.dirname(__FILE__) + '/../spec_helper'

context "Parser" do
  specify "should parse a symbol literal that uses single quotes" do
    :'$4 for one or two'.should == :'$4 for one or two'
    :'&smack'.should == :'&smack'
  end
end
