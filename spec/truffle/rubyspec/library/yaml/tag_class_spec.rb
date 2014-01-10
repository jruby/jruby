require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

ruby_version_is "" ... "2.0" do
  describe "YAML.tag_class" do
    it "associates a taguri tag with a ruby class" do
      YAML.tag_class('rubini.us','rubinius').should == "rubinius"
    end
  end
end
