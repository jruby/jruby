require 'java'
require 'rspec/autorun'

virtual = ARGV.shift == 'true'

class ClosingScheduler
  attr_reader :closed

  def block(*)
    raise 'unexpected scheduler block'
  end

  def unblock(*)
    raise 'unexpected scheduler unblock'
  end

  def kernel_sleep(*)
    raise 'unexpected scheduler sleep'
  end

  def io_wait(*)
    raise 'unexpected scheduler IO'
  end

  def scheduler_close
    @closed = Fiber.new { :closed }.resume
  end
end

RSpec.describe "Scheduler shutdown with thread.vthread=#{virtual}" do
  it 'finishes fiber work in the close hook before the thread can be joined' do
    scheduler = ClosingScheduler.new
    thread = Thread.new do
      expect(java.lang.Thread.current_thread.virtual?).to eq(virtual)
      Fiber.set_scheduler(scheduler)
      :finished
    end

    expect(thread.value).to eq(:finished)
    expect(scheduler.closed).to eq(:closed)
    expect(thread.status).to eq(false)
  end
end
