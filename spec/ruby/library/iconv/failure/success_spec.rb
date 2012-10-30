require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes.rb', __FILE__)

ruby_version_is ''...'2.0' do 
  describe "Iconv::Failure#success" do
    it "for Iconv#iconv and Iconv.conv returns the substring of the original string passed which was translated successfully until the exception ocurred" do
      lambda {
        begin
          Iconv.open "utf-8", "utf-8" do |conv|
            conv.iconv "test \xff test \xff"
          end
        rescue Iconv::Failure => e
          @ex = e
          raise e
        end
      }.should raise_error(Iconv::Failure)
      @ex.success.should == "test "

      lambda {
        begin
          Iconv.conv "utf-8", "utf-8", "\xe2\x82"
        rescue Iconv::Failure => e
          @ex = e
          raise e
        end
      }.should raise_error(Iconv::Failure)
      @ex.success.should == ""
    end

    it "for Iconv.iconv returns an array containing all the strings that were translated successfully until the exception ocurred, in order" do
      lambda {
        begin
          Iconv.iconv("utf-8", "utf-8", "\xfferror")
        rescue Iconv::Failure => e
          @ex = e
          raise e
        end
      }.should raise_error(Iconv::Failure)
      @ex.success.should == [""]

      lambda {
        begin
          Iconv.iconv("utf-8", "utf-8", "test", "testing", "until\xfferror")
        rescue Iconv::Failure => e
          @ex = e
          raise e
        end
      }.should raise_error(Iconv::Failure)
      @ex.success.should == ["test", "testing", "until"]
    end
  end
end
