require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "UnboundMethod#arity" do
  before(:each) do
    @um = UnboundMethodSpecs::Methods.new
  end

  it "returns the number of arguments accepted by a method, using Method#unbind" do
    @um.method(:one).unbind.arity.should == 0
    @um.method(:two).unbind.arity.should == 1
    @um.method(:three).unbind.arity.should == 2
    @um.method(:four).unbind.arity.should == 2
  end

  it "returns the number arguments accepted by a method, using Module#instance_method" do
    UnboundMethodSpecs::Methods.instance_method(:one).arity.should == 0
    UnboundMethodSpecs::Methods.instance_method(:two).arity.should == 1
    UnboundMethodSpecs::Methods.instance_method(:three).arity.should == 2
    UnboundMethodSpecs::Methods.instance_method(:four).arity.should == 2
  end

  it "if optional arguments returns the negative number of mandatory arguments, using Method#unbind" do
    @um.method(:neg_one).unbind.arity.should == -1
    @um.method(:neg_two).unbind.arity.should == -2
    @um.method(:neg_three).unbind.arity.should == -3
    @um.method(:neg_four).unbind.arity.should == -3
  end

  it "if optional arguments returns the negative number of mandatory arguments, using Module#instance_method" do
    UnboundMethodSpecs::Methods.instance_method(:neg_one).arity.should == -1
    UnboundMethodSpecs::Methods.instance_method(:neg_two).arity.should == -2
    UnboundMethodSpecs::Methods.instance_method(:neg_three).arity.should == -3
    UnboundMethodSpecs::Methods.instance_method(:neg_four).arity.should == -3
  end
end
