require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#untrusted?" do
  ruby_version_is "1.9" do
    it "returns the untrusted status of an object" do
      o = mock('o')
      o.untrusted?.should == false
      o.untrust
      o.untrusted?.should == true
    end

    it "has no effect on immediate values" do
      a = nil
      b = true
      c = false
      d = 1
      a.untrust
      b.untrust
      c.untrust
      d.untrust
      a.untrusted?.should == false
      b.untrusted?.should == false
      c.untrusted?.should == false
      d.untrusted?.should == false
    end
  end
end
