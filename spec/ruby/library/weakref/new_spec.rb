require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "WeakRef#new" do
  it "creates a subclass correctly" do
    wr2 = Class.new(WeakRef) {
      def __getobj__
        :dummy
      end
    }
    wr2.new(Object.new).__getobj__.should == :dummy
  end

  it "does not keep the WeakRef itself alive while its object is" do
    obj = Object.new
    map = ObjectSpace::WeakMap.new
    WeakRefSpec.put_unreferenced_weakref(map, obj)

    WeakRefSpec.collect_until { map.size == 0 }.should == true
    obj.should_not == nil # keep the referenced object alive until here
  end
end
