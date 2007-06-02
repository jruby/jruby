require File.dirname(__FILE__) + '/../spec_helper'

context "ByteArray instance method" do
  specify "[] should return the byte at index" do
    s= "A92f"
    b = s.data
    a = []
    0.upto(s.size-1) { |i| a << b[i].chr }
    a.should == ["A", "9", "2", "f"]
  end
  
  specify "[]= should set the byte at index" do
    s = "01234"
    b = s.data
    0.upto(s.size-1) { |i| b[i] = b[i] + ?A }
    s.should == "qrstu"
  end
end