require 'rspec'
require 'timeout'

# https://github.com/jruby/jruby/issues/9972
describe 'Mutex#synchronize interrupted while waiting on a contended lock' do
  it 'does not leave the lock held after the interrupt' do
    m = Mutex.new
    leaked = Queue.new
    deadline = Time.now + 5

    threads = 4.times.map do
      Thread.new do
        Thread.handle_interrupt(Object => :never) do
          while Time.now < deadline
            begin
              Thread.handle_interrupt(Object => :immediate) do
                Timeout.timeout(0.00001) { 200.times { m.synchronize { 1 + 1 } } }
              end
            rescue Timeout::Error
            end

            # outside every synchronize block, so the lock must not be ours
            if m.owned?
              leaked << Thread.current
              m.unlock
              break
            end
          end
        end
      end
    end

    threads.each(&:join)

    expect(leaked.size).to eq(0)
  end
end
