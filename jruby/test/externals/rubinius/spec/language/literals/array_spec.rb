require File.dirname(__FILE__) + '/../../spec_helper'

context "Array literal" do
  specify "[] should return a new array populated with the given elements" do
    [1, 2, 3, 'a', 'b', 5].should == [1, 2, 3, "a", "b", 5]
  end

  specify "%w() should also return a new array" do
    ip = 'xxx'
    %w(sk jsjvkdfnv #{ip} kvdjf 3).should == ["sk", "jsjvkdfnv", "\#{ip}", "kvdjf", "3"]
  end

  specify "%W() is like %w() but should support interpolation" do
    ip = 'xxx'
    %W(skdf #{ip}x).should == ["skdf", "xxxx"]
  end
end
