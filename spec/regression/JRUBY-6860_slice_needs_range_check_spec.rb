# encoding: utf-8

if RUBY_VERSION =~ /1\.9/
  describe "JRUBY-6860: String#slice" do
    it "checks range properly when given begin or length outside actual" do
      'å'.slice(0,16).should == "å"
      'å'.slice(0,17).should == "å"
      'å'.slice(1,16).should == ""
      'å'.slice(1,17).should == ""

      '1234567890å'.slice(0,17).should == "1234567890å"
      '1234567890å'.slice(1,17).should == "234567890å"
      '1234567890å'.slice(9,17).should == "0å"
    end
  end
end
