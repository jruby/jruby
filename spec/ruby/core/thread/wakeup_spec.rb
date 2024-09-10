require_relative '../../spec_helper'
require_relative 'fixtures/classes'
require_relative 'shared/wakeup'

describe "Thread#wakeup" do
  it_behaves_like :thread_wakeup, :wakeup

  it "sleeps with nanosecond precision" do
    start_time = Process.clock_gettime(Process::CLOCK_MONOTONIC)
    100.times do
      sleep(0.0001)
    end
    end_time = Process.clock_gettime(Process::CLOCK_MONOTONIC)

    actual_duration = end_time - start_time
    (actual_duration > 0.01).should == true # 100 * 0.0001 => 0.01
    (actual_duration < 0.03).should == true
  end
end
