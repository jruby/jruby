require File.expand_path('../../../spec_helper', __FILE__)

describe "ENV.each_value" do

  it "returns each value" do
    e = []
    orig = ENV.to_hash
    begin
      ENV.clear
      ENV["1"] = "3"
      ENV["2"] = "4"
      ENV.each_value { |v| e << v }
      e.should include("3")
      e.should include("4")
    ensure
      ENV.replace orig
    end
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises LocalJumpError if no block given" do
      lambda { ENV.each_value }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator if called without a block" do
      ENV.each_value.should be_an_instance_of(enumerator_class)
    end
  end

  ruby_version_is "1.9" do
    it "uses the locale encoding" do
      ENV.each_value do |value|
        value.encoding.should == Encoding.find('locale')
      end
    end
  end
end
