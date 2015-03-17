require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Hash#keys" do

  it "returns an array with the keys in the order they were inserted" do
    new_hash.keys.should == []
    new_hash.keys.should be_kind_of(Array)
    new_hash(5).keys.should == []
    new_hash { 5 }.keys.should == []
    new_hash(1 => 2, 4 => 8, 2 => 4).keys.should == [1, 4, 2]
    new_hash(1 => 2, 2 => 4, 4 => 8).keys.should be_kind_of(Array)
    new_hash(nil => nil).keys.should == [nil]
  end

  it "uses the same order as #values" do
    h = new_hash(1 => "1", 2 => "2", 3 => "3", 4 => "4")

    h.size.times do |i|
      h[h.keys[i]].should == h.values[i]
    end
  end
end
