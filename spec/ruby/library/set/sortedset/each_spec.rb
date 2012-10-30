require File.expand_path('../../../../spec_helper', __FILE__)
require 'set'

describe "SortedSet#each" do
  before(:each) do
    @set = SortedSet[1, 2, 3]
  end

  it "yields each Object in self in sorted order" do
    ret = []
    SortedSet["one", "two", "three"].each { |x| ret << x }
    ret.should == ["one", "two", "three"].sort
  end

  ruby_bug "http://redmine.ruby-lang.org/issues/show/116", "1.8.7.7" do
    it "returns self" do
      @set.each { |x| x }.should equal(@set)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator when not passed a block" do
      enum = @set.each

      ret = []
      enum.each { |x| ret << x }
      ret.sort.should == [1, 2, 3]
    end
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises a LocalJumpError when not passed a block" do
      lambda { @set.each }.should raise_error(LocalJumpError)
    end
  end
end
