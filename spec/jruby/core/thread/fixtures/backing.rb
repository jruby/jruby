require 'java'
require 'rspec/autorun'

virtual = ARGV.shift == 'true'

RSpec.describe "Ruby Thread with thread.vthread=#{virtual}" do
  it 'uses the selected Java backing and initializes identity and group before execution' do
    group = ThreadGroup.new
    thread, native = Thread.new do
      group.add(Thread.current)
      child = Thread.new do
        current = Thread.current
        backing = java.lang.Thread.current_thread
        expect(backing.virtual?).to eq(virtual)
        expect(backing.daemon?).to eq(true)
        expect(current.to_java(java.lang.Thread)).to equal(backing)
        expect(current.group).to equal(group)
        expect(Thread.list).to include(current)
        expect(group.list).to include(current)
        backing
      end
      [child, child.value]
    end.value

    expect(thread.join).to equal(thread)
    expect(thread.status).to eq(false)
    expect(native.alive?).to eq(false)
    expect(Thread.list).not_to include(thread)
    expect(group.list).not_to include(thread)
  end

  it 'retains Ruby priority changes and inherits them in children' do
    Thread.new do
      Thread.current.priority = 2
      Thread.new do
        expect(Thread.current.priority).to eq(2)
        Thread.current.priority = -2
        expect(Thread.current.priority).to eq(-2)
        expect(Thread.new { Thread.current.priority }.value).to eq(-2)
      end.value
    end.value
  end

  it 'inherits an interrupt mask before executing and delivers the deferred interrupt when unmasked' do
    release = Queue.new
    thread = Thread.handle_interrupt(RuntimeError => :never) do
      Thread.new do
        release.pop
        expect do
          Thread.handle_interrupt(RuntimeError => :immediate) { }
        end.to raise_error(RuntimeError, 'deferred')
        :finished
      end
    end
    thread.raise(RuntimeError, 'deferred')
    release << true
    expect(thread.value).to eq(:finished)
  ensure
    thread&.kill&.join
  end
end
