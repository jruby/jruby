# -*- encoding: us-ascii -*-

require File.expand_path('../../../spec_helper', __FILE__)

describe "Module#prepended" do
  it "is a private method" do
    Module.should have_private_instance_method(:prepended, true)
  end
end
