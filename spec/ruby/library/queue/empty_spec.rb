require File.expand_path('../../../spec_helper', __FILE__)
require 'thread'

describe "Queue#empty?" do
  it "returns true on an empty Queue" do
    queue = Queue.new
    queue.empty?.should be_true
  end

  it "returns false when Queue is not empty" do
    queue = Queue.new
    queue << Object.new
    queue.empty?.should be_false
  end
end
