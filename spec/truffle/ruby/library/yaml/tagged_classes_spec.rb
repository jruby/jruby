require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

ruby_version_is "" ... "2.0" do
  describe "YAML.tagged_classes" do
    it "returns a complete dictionary of taguris paired with classes" do
      YAML.tagged_classes["tag:yaml.org,2002:int"].should == Integer
    end
  end
end
