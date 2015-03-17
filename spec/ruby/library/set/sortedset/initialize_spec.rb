require File.expand_path('../../../../spec_helper', __FILE__)
require 'set'

describe "SortedSet#initialize" do
  it "is private" do
    SortedSet.should have_private_instance_method("initialize")
  end

  it "adds all elements of the passed Enumerable to self" do
    s = SortedSet.new([1, 2, 3])
    s.size.should eql(3)
    s.should include(1)
    s.should include(2)
    s.should include(3)
  end

  it "preprocesses all elements by a passed block before adding to self" do
    s = SortedSet.new([1, 2, 3]) { |x| x * x }
    s.size.should eql(3)
    s.should include(1)
    s.should include(4)
    s.should include(9)
  end

  ruby_version_is ""..."1.9" do
    ruby_bug "redmine #118", "1.8.7" do
      it "takes only values which respond to <=>" do
        lambda { SortedSet[3, 4, SortedSet[5, 6]] }.should raise_error(ArgumentError)
      end
    end
  end
end
