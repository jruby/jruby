require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#initialize_copy" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:initialize_copy)
  end
end
