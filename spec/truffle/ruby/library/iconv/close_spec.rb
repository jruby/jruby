require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

ruby_version_is ''...'2.0' do 
  describe "Iconv#close" do
    it "ignores multiple calls" do
      conv1 = Iconv.new("us-ascii", "us-ascii")
      conv1.close.should == ""
      conv1.close.should be_nil
    end

    it "does not raise an exception if called inside an .open block" do
      Iconv.open "us-ascii", "us-ascii" do |conv2|
        conv2.close.should == ""
      end
    end

    it "returns a string containing the byte sequence to change the output buffer to its initial shift state" do
      Iconv.open "ISO-2022-JP", "UTF-8" do |cd|
        cd.iconv("\343\201\262")
        cd.close.should == encode("\e(B", "iso-2022-jp")
      end
    end
  end
end
