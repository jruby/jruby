require 'tempfile'
require 'thread'

describe 'JRUBY-3343: threaded require' do
  before :all do
    @file = Tempfile.open(['sleeper', '.rb'])
    @file.puts "sleep 0.5"
    @file.close
  end

  after :all do
    @file.delete
  end

  it 'should block a second thread requiring a file currently being required' do
    queue = Queue.new
    t1 = Thread.new(queue) { |q1|
      Thread.abort_on_exception = true
      r1 = require @file.path
      q1 << [:first, r1]
    }
    t2 = Thread.new(queue) { |q2|
      Thread.abort_on_exception = true
      Thread.pass until t1.status == "sleep" # hope it detects the status while 0.5 sec sleep... hackish.
      r2 = require @file.path
      q2 << [:second, r2]
    }
    queue.pop.should == [:first, true]
    queue.pop.should == [:second, false]
    t1.join
    t2.join
  end
end
