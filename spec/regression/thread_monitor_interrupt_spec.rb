require 'rspec'
require 'java'
require 'jruby'
require 'jruby/util'

describe 'Interrupting a Ruby thread waiting on a Java monitor' do
  [:kill, :raise, :wakeup].product([0, 1]).each do |operation, index|
    it "delivers #{operation} to waiter #{index} when the monitor is shared" do
      monitor = java.lang.Object.new
      ready = Queue.new
      resumed = Queue.new
      raised = Queue.new
      stopped = false
      threads = []
      error = Class.new(StandardError)

      begin
        2.times do
          threads << Thread.new do
            begin
              JRuby::Util.synchronized(monitor) do
                ready << true
                until stopped
                  JRuby.reference(Thread.current).wait_for_object(monitor)
                  resumed << Thread.current
                end
              end
            rescue error
              raised << Thread.current
            end
          end
          ready.pop
          # The worker holds this monitor until Object.wait releases it.
          JRuby::Util.synchronized(monitor) {}
        end

        # A waiter must not consume an interrupt meant for the other thread.
        target = threads[index]
        case operation
        when :kill
          target.kill
          expect(target.join(5)).to eq(target)
        when :raise
          target.raise(error)
          expect(target.join(5)).to eq(target)
          expect(raised.pop).to eq(target)
        when :wakeup
          target.wakeup
          # A bounded join on an observer keeps a lost wakeup a failure,
          # while allowing unrelated waiters to resume spuriously.
          observer = Thread.new { loop { break if resumed.pop == target } }
          threads << observer
          expect(observer.join(5)).to eq(observer)
        end
        expect(threads[1 - index]).to be_alive
      ensure
        JRuby::Util.synchronized(monitor) do
          stopped = true
          monitor.notify_all
        end
        threads.each { |thread| thread.kill; thread.join(5) }
      end
    end
  end
end
