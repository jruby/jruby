require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Enumerable#reject" do
  it "returns an array of the elements for which block is false" do
    EnumerableSpecs::Numerous.new.reject { |i| i > 3 }.should == [2, 3, 1]
    entries = (1..10).to_a
    numerous = EnumerableSpecs::Numerous.new(*entries)
    numerous.reject {|i| i % 2 == 0 }.should == [1,3,5,7,9]
    numerous.reject {|i| true }.should == []
    numerous.reject {|i| false }.should == entries
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises a LocalJumpError if no block is given" do
      lambda { EnumerableSpecs::Numerous.new.reject }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator if called without a block" do
      EnumerableSpecs::Numerous.new.reject.should be_an_instance_of(enumerator_class)
    end
  end

  it "gathers whole arrays as elements when each yields multiple" do
    multi = EnumerableSpecs::YieldsMulti.new
    multi.reject {|e| e == [3, 4, 5] }.should == [[1, 2], [6, 7, 8, 9]]
  end

end

