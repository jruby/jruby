require File.expand_path('../../../spec_helper', __FILE__)

describe "ObjectSpace.each_object" do
  it "calls the block once for each living, non-immediate object in the Ruby process" do
    class ObjectSpaceSpecEachObject; end
    new_obj = ObjectSpaceSpecEachObject.new

    yields = 0
    count = ObjectSpace.each_object(ObjectSpaceSpecEachObject) do |obj|
      obj.should == new_obj
      yields += 1
    end
    count.should == 1
    yields.should == 1

    # this is needed to prevent the new_obj from being GC'd too early
    new_obj.should_not == nil
  end

  it "calls the block once for each class, module in the Ruby process" do
    class ObjectSpaceSpecEachClass; end
    module ObjectSpaceSpecEachModule; end

    [ObjectSpaceSpecEachClass, ObjectSpaceSpecEachModule].each do |k|
      yields = 0
      got_it = false
      count = ObjectSpace.each_object(k.class) do |obj|
        got_it = true if obj == k
        yields += 1
      end
      got_it.should == true
      count.should == yields
    end
  end

  ruby_version_is '1.8.7' do
    it "returns an enumerator if not given a block" do
      class ObjectSpaceSpecEachOtherObject; end
      new_obj = ObjectSpaceSpecEachOtherObject.new

      counter = ObjectSpace.each_object(ObjectSpaceSpecEachOtherObject)
      counter.should be_an_instance_of(enumerator_class)
      counter.each{}.should == 1
      # this is needed to prevent the new_obj from being GC'd too early
      new_obj.should_not == nil
    end
  end
end
