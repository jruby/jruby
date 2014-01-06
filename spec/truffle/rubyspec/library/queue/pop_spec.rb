require File.expand_path('../../../spec_helper', __FILE__)
require 'thread'
require File.expand_path('../shared/deque', __FILE__)

describe "Queue#pop" do
  it_behaves_like :queue_deq, :pop
end
