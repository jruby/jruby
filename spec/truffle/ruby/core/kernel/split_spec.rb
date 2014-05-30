require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Kernel#split" do
    it "is a private method" do
      Kernel.should have_private_instance_method(:split)
    end
  end

  describe "Kernel.split" do
    it "needs to be reviewed for spec completeness"
  end
end
