require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/new', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

ruby_version_is ''...'2.0' do 
  describe "Iconv.open" do
    it_behaves_like :iconv_new, :open

    it "with a block invokes the block exactly once" do
      count = 0
      Iconv.open "us-ascii", "us-ascii" do
        count += 1
      end
      count.should == 1
    end

    it "with a block yields the converter" do
      Iconv.open "us-ascii", "us-ascii" do |conv|
        conv.should be_kind_of(Iconv)
      end
    end

    it "with a block returns the result of the block" do
      Iconv.open("us-ascii", "us-ascii") { "block return value" }.should == "block return value"
    end

    # not testable with the current API:
    # it "with a block always closes the converter when exiting the block"
  end
end
