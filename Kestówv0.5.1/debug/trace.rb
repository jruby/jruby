# frozen_string_literal: true

# Kestówv 0.5.1 - debug/trace.rb
#
# Execution tracing.
# Registers trace features.

module Kestowv
  module Debug
    module Trace
      @traces  = []
      @enabled = false
      @mutex   = Mutex.new

      class << self
        def register_features
          Boot.register(:debug_trace)
          Boot.set_bit(:debug_trace)
        end

        def enable
          @enabled = true
        end

        def disable
          @enabled = false
        end

        def record(event)
          return unless @enabled
          @mutex.synchronize do
            @traces << { time: Time.now, event: event }
          end
        end

        def recent(count = 20)
          @traces.last(count)
        end

        def to_a
          @traces
        end

        def stats
          {
            feature: :debug_trace,
            count:   @traces.size,
            enabled: @enabled
          }
        end
      end
    end
  end
end
# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :debug_trace,
  __FILE__,
  feature:    :debug_trace,
  depends_on: []
)
