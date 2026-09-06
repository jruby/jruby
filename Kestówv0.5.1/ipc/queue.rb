# frozen_string_literal: true

# Kestówv 0.5.1 - ipc/queue.rb
#
# Generic message queue.
# Registers queue features.

module Kestowv
  module Ipc
    module Queue
      @queues = {}
      @heads  = Hash.new(0)   # key => head index into @queues[key]
      @mutex  = Mutex.new

      class << self
        def register_features
          Boot.register(:ipc_queue)
          Boot.set_bit(:ipc_queue)
        end

        def create(key)
          @mutex.synchronize { @queues[key] ||= [] }
        end

        def enqueue(key, item)
          @mutex.synchronize { @queues[key] << item }
        end

        def dequeue(key)
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
          @queues.keys
        end

        def stats
          {
            feature: :ipc_queue,
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
  :ipc_queue,
  __FILE__,
  feature:    :ipc_queue,
  depends_on: [:ipc]
)
