require File.expand_path('../../../spec_helper', __FILE__)

describe "ObjectSpace._id2ref" do
  it "converts an object id to a reference to the object" do
    s = "I am a string"
    r = ObjectSpace._id2ref(s.object_id)
    r.should == s
  end
end
