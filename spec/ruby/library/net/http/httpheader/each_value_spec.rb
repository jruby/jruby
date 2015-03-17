require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/classes', __FILE__)

describe "Net::HTTPHeader#each_value" do
  before(:each) do
    @headers = NetHTTPHeaderSpecs::Example.new
    @headers["My-Header"] = "test"
    @headers.add_field("My-Other-Header", "a")
    @headers.add_field("My-Other-Header", "b")
  end

  describe "when passed a block" do
    it "yields each header entry's joined values" do
      res = []
      @headers.each_value do |value|
        res << value
      end
      res.sort.should == ["a, b", "test"]
    end
  end

  describe "when passed no block" do
    ruby_version_is "" ... "1.8.7" do
      it "raises a LocalJumpError" do
        lambda { @headers.each_value }.should raise_error(LocalJumpError)
      end
    end

    # TODO: This should return an Enumerator and not raise an Error
    ruby_version_is "1.8.7" do
      ruby_bug "http://redmine.ruby-lang.org/issues/show/447", "1.8.7" do
        it "returns an Enumerator" do
          enumerator = @headers.each_value
          enumerator.should be_an_instance_of(enumerator_class)

          res = []
          enumerator.each do |key|
            res << key
          end
          res.sort.should == ["a, b", "test"]
        end
      end
    end
  end
end
