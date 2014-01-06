require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#frozen?" do
  it "returns true if self is frozen" do
    o = mock('o')
    p = mock('p')
    p.freeze
    o.frozen?.should == false
    p.frozen?.should == true
  end
end
