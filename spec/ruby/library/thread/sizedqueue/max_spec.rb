require File.expand_path('../../../../spec_helper', __FILE__)
require 'thread'

describe "Thread::SizedQueue#max" do
  before :each do
    @sized_queue = SizedQueue.new(5)
  end

  it "returns the size of the queue" do
    @sized_queue.max.should == 5
  end
end

describe "Thread::SizedQueue#max=" do
  before :each do
    @sized_queue = SizedQueue.new(5)
  end

  it "sets the size of the queue" do
    @sized_queue.max.should == 5
    @sized_queue.max = 10
    @sized_queue.max.should == 10
  end

  ruby_version_is ""..."2.1" do
    it "raises a NoMethodError when a value without #>" do
      lambda { @sized_queue.max = Object.new }.should raise_error(NoMethodError)
    end

    it "raises an ArgumentError for values not comparable to a number" do
      lambda { @sized_queue.max = "foo" }.should raise_error(ArgumentError)
    end
  end

  ruby_version_is "2.1" do
    it "raises a TypeError when given a non-numeric value" do
      lambda { @sized_queue.max = "foo" }.should raise_error(TypeError)
      lambda { @sized_queue.max = Object.new }.should raise_error(TypeError)
    end
  end

  it "raises an argument error when set to zero" do
    @sized_queue.max.should == 5
    lambda { @sized_queue.max = 0 }.should raise_error(ArgumentError)
    @sized_queue.max.should == 5
  end

  it "raises an argument error when set to a negative number" do
    @sized_queue.max.should == 5
    lambda { @sized_queue.max = -1 }.should raise_error(ArgumentError)
    @sized_queue.max.should == 5
  end
end
