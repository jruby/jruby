require File.expand_path('../../../../spec_helper', __FILE__)
require 'thread'
require File.expand_path('../../shared/queue/num_waiting', __FILE__)

describe "Thread::SizedQueue#num_waiting" do
  it_behaves_like :queue_num_waiting, :num_waiting, SizedQueue.new(10)
end
