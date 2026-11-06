# frozen_string_literal: true

# Kestówv 0.5.1 - ipc/msg.rb
#
# Message queue simulation.
# Registers message queue features.

module Kestowv
  module Ipc
    module Msg
      @queues = {}
      @heads  = Hash.new(0)   # key => head index into @queues[key]
      @mutex  = Mutex.new

      class << self
        def register_features
          Boot.register(:ipc_msg)
          Boot.set_bit(:ipc_msg)
        end

        def create(key)
          @mutex.synchronize { @queues[key] ||= [] }
        end

        # Avoid collision with Kernel#send
        def enqueue(key, message)
          @mutex.synchronize { @queues[key] << message }
        end

        def receive(key)
          @mutex.synchronize do
            arr  = @queues[key]
            return nil unless arr
            head = @heads[key]
            return nil if head >= arr.size
            item = arr[head]
            arr[head] = nil
            @heads[key] = head + 1
            if head + 1 > 32 && head + 1 > arr.size / 2
              @queues[key] = arr[(head + 1)..]
              @heads[key]  = 0
            end
            item
          end
        end

        def to_a
          @queues
        end

        def stats
          {
            feature: :ipc_msg,
            queues:  @queues.size
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
  :ipc_msg,
  __FILE__,
  feature:    :ipc_msg,
  depends_on: [:ipc]
)
