require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe Object, "#responds_to" do
  it "returns true for specified symbols" do
    obj = double("obj")
    obj.responds_to(:to_flo)
    obj.should respond_to(:to_flo)
    obj.should respond_to(:to_s)
  end
end

describe Object, "#does_not_respond_to" do
  it "returns false for specified symbols" do
    obj = double("obj")
    obj.does_not_respond_to(:to_s)
    obj.should_not respond_to(:to_s)
  end
end

describe Object, "#undefine" do
  it "undefines the method" do
    # cannot use a mock here because of the way RSpec handles method_missing
    obj = Object.new
    obj.undefine(:to_s)
    lambda { obj.send :to_s }.should raise_error(NoMethodError)
  end
end

describe Object, "#fake!" do
  before :each do
    @obj = double("obj")
  end

  it "makes the object respond to the message" do
    @obj.fake!(:to_flo)
    @obj.should respond_to(:to_flo)
  end

  it "returns the value when the obj is sent the message" do
    @obj.fake!(:to_flo, 1.2)
    @obj.to_flo.should == 1.2
  end
end
