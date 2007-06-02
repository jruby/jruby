require File.dirname(__FILE__) + '/../spec_helper'

context "Compression::ZLib" do
  specify "inflate should uncompress data" do
    data = Compression::ZLib.deflate("blah")
    Compression::ZLib.inflate(data).should == 'blah'
  end
  
  specify "deflate should convert other to compressed data" do
    Compression::ZLib.deflate("blah").should == "x\234K\312I\314\000\000\003\372\001\230"
  end
end