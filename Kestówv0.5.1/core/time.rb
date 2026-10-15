# frozen_string_literal: true

# Kestówv 0.5.1 — core/time.rb
#
# Time and jiffies tracking with Boot integration.

module Kestowv
  module Core
    module Time

      @jiffies    = 0
      @start_time = Process.clock_gettime(Process::CLOCK_MONOTONIC)
      @mutex      = Mutex.new

      class << self

        def register
          Boot.register(:time_jiffies)
          Boot.set_bit(:time_jiffies)
        end

        def tick
          @mutex.synchronize { @jiffies += 1 }
        end

        def jiffies
          @mutex.synchronize { @jiffies }
        end

        def uptime
          Process.clock_gettime(Process::CLOCK_MONOTONIC) - @start_time
        end

        def to_a
          @mutex.synchronize do
            {
              jiffies:        @jiffies,
              uptime_seconds: uptime
            }
          end
        end

        def stats
          @mutex.synchronize do
            {
              jiffies: @jiffies,
              feature: :time_jiffies
            }
          end
        end
      end
    end
  end
end

Kestowv::Config::Modules.register(
  :core_time,
  __FILE__,
  feature: :time
)
