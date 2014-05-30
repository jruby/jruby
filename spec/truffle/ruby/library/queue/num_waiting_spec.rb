require File.expand_path('../../../spec_helper', __FILE__)
require 'thread'

describe "Queue#num_waiting" do
  it "reports the number of threads waiting on the Queue" do
    q = Queue.new
    threads = []

    5.times do |i|
      q.num_waiting.should == i
      t = Thread.new { q.deq }
      Thread.pass until t.status == 'sleep'
      threads << t
    end

    threads.each { q.enq Object.new }
    threads.each {|t| t.join }
  end
end
