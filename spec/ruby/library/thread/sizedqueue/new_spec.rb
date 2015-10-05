require File.expand_path('../../../../spec_helper', __FILE__)
require 'thread'

describe "Thread::SizedQueue#new" do
  it "returns a new SizedQueue" do
    SizedQueue.new(1).should be_kind_of(SizedQueue)
  end

  ruby_version_is ""..."2.1" do
    it "raises a NoMethodError when an argument without #> is given" do
      lambda { SizedQueue.new(Object.new) }.should raise_error(NoMethodError)
    end

    it "raises an ArgumentError for arguments not comparable to a number" do
      lambda { SizedQueue.new("foo") }.should raise_error(ArgumentError)
    end
  end

  ruby_version_is "2.1" do
    it "raises a TypeError when the given argument is not Numeric" do
      lambda { SizedQueue.new("foo") }.should raise_error(TypeError)
      lambda { SizedQueue.new(Object.new) }.should raise_error(TypeError)
    end
  end

  it "raises an argument error when no argument is given" do
    lambda { SizedQueue.new }.should raise_error(ArgumentError)
  end

  it "raises an argument error when the given argument is zero" do
    lambda { SizedQueue.new(0) }.should raise_error(ArgumentError)
  end

  it "raises an argument error when the given argument is negative" do
    lambda { SizedQueue.new(-1) }.should raise_error(ArgumentError)
  end
end
