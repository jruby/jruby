require File.dirname(__FILE__) + '/../spec_helper'

# Methods
# _id2ref, add_finalizer, call_finalizer, define_finalizer, each_object,
# finalizers, garbage_collect, remove_finalizer, undefine_finalizer

context "ObjectSpace class methods" do
  specify "_id2ref should convert an object id to a reference to the object" do
    s = "I am a string"
    r = ObjectSpace._id2ref(s.object_id)
    r.should == s
  end

  specify "each_object should call the block once for each living, nonimmediate object in the Ruby process" do
    class ObjectSpaceSpecEachObject; end
    ObjectSpaceSpecEachObject.new

    count = ObjectSpace.each_object(ObjectSpaceSpecEachObject) {}
    count.should == 1
  end
end
