require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/dup_clone', __FILE__)

describe "Object#clone" do
  it_behaves_like :object_dup_clone, :clone

  it "preserves frozen state from the original" do
    o = ObjectSpecDupInitCopy.new
    o2 = o.clone
    o.freeze
    o3 = o.clone

    o2.frozen?.should == false
    o3.frozen?.should == true
  end
end

