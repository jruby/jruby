require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/dup_clone', __FILE__)

describe "Object#dup" do
  it_behaves_like :object_dup_clone, :dup

  it "does not preserve frozen state from the original" do
    o = ObjectSpecDupInitCopy.new
    o.freeze
    o2 = o.dup

    o2.frozen?.should == false
  end
end
