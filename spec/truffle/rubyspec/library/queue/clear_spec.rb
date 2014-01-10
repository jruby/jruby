require File.expand_path('../../../spec_helper', __FILE__)
require 'thread'

describe "Queue#clear" do
  it "removes all objects from the queue" do
    queue = Queue.new
    queue << Object.new
    queue << 1
    queue.empty?.should be_false
    queue.clear
    queue.empty?.should be_true
  end

  # TODO: test for atomicity of Queue#clear
end
