require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "ObjectSpace::WeakMap" do

  # Note that we can't really spec the most important aspect of this class: that entries get removed when the values
  # become unreachable. This is because Ruby does not offer a way to reliable invoke GC (GC.start is not enough, neither
  # on MRI or on alternative implementations).

  it "includes Enumerable" do
    ObjectSpace::WeakMap.include?(Enumerable).should == true
  end

  it "removes an entry once its key has been garbage collected" do
    map = ObjectSpace::WeakMap.new
    value = Object.new
    ObjectSpaceFixtures.weakmap_add_unreachable_key(map, value)

    ObjectSpaceFixtures.collect_until { map.size == 0 }.should == true
    map.keys.should == []
    map.values.should == []
    map.each { |k, v| raise "entry #{k.inspect} => #{v.inspect} survived its key" }
    value.should_not == nil # keep the value alive until here so only the key was collectable
  end

  it "removes an entry once its value has been garbage collected" do
    map = ObjectSpace::WeakMap.new
    key = Object.new
    ObjectSpaceFixtures.weakmap_add_unreachable_value(map, key)

    ObjectSpaceFixtures.collect_until { !map.key?(key) }.should == true
    map[key].should == nil
    map.size.should == 0
    key.should_not == nil # keep the key alive until here so only the value was collectable
  end
end
