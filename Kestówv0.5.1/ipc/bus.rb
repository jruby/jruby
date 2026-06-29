# frozen_string_literal: true

# Kestówv 0.5.1 - ipc/bus.rb
#
# Event bus for inter-component communication.
# Registers bus features.

module Kestowv
  module Ipc
    module Bus
      @subscribers = {}
      @mutex       = Mutex.new

      class << self
        def register_features
          Boot.register(:ipc_bus)
          Boot.set_bit(:ipc_bus)
        end

        def subscribe(event, &block)
          @mutex.synchronize do
            @subscribers[event] ||= []
            @subscribers[event] << block
          end
        end

        def publish(event, *args)
          (@subscribers[event] || []).each { |h| h.call(*args) }
        end

        def to_a
          @subscribers.keys
        end

        def stats
          {
            feature: :ipc_bus,
            events:  @subscribers.size
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
  :ipc_bus,
  __FILE__,
  feature:    :ipc_bus,
  depends_on: [:ipc]
)
