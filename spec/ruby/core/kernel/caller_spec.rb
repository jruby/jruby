require File.expand_path('../../../spec_helper', __FILE__)

describe "Kernel#caller" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:caller)
  end
end
