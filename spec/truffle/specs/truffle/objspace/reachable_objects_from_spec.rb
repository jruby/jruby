require_relative '../../../../ruby/spec_helper'
require 'objspace'

# Truffle-specific behavior
describe "ObjectSpace.reachable_objects_from" do
  it "enumerates objects directly reachable from a given object" do
    ObjectSpace.reachable_objects_from(nil).should == [NilClass]
    ObjectSpace.reachable_objects_from(['a', 'b', 'c']).should include(Array, 'a', 'b', 'c')
    ObjectSpace.reachable_objects_from(Object.new).should == [Object]
  end

  it "finds the superclass and included/prepended modules of a class" do
    superclass = Class.new
    klass = Class.new(superclass)
    included = Module.new
    klass.include included
    prepended = Module.new
    klass.prepend prepended
    ObjectSpace.reachable_objects_from(klass).should include(prepended, included, superclass)
  end

  it "finds a variable captured by a block captured by #define_method" do
    captured = Object.new
    obj = Object.new
    block = lambda {
      captured
    }
    obj.singleton_class.send(:define_method, :capturing_method, block)

    meth = obj.method(:capturing_method)
    reachable = ObjectSpace.reachable_objects_from(meth)
    reachable.should include(Method, obj)

    reachable = reachable + reachable.flat_map { |r| ObjectSpace.reachable_objects_from(r) }
    reachable.should include(captured)
  end

  it "finds an object stored in a Queue" do
    require 'thread'
    o = Object.new
    q = Queue.new
    q << o

    reachable = ObjectSpace.reachable_objects_from(q)
    reachable.should include(o)
  end

  it "finds an object stored in a SizedQueue" do
    require 'thread'
    o = Object.new
    q = SizedQueue.new(3)
    q << o

    reachable = ObjectSpace.reachable_objects_from(q)
    reachable.should include(o)
  end
end
