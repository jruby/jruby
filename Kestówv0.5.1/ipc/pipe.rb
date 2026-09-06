# frozen_string_literal: true

# Kestówv 0.5.1 - ipc/pipe.rb
#
# Pipe simulation.
# Registers pipe features.

module Kestowv
  module Ipc
    module Pipe
      @pipes = {}
      @heads = Hash.new(0)   # id => head index into @pipes[id]
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:ipc_pipe)
          Boot.set_bit(:ipc_pipe)
        end

        def create
          id = SecureRandom.hex(4)
          @mutex.synchronize { @pipes[id] = [] }
          id
        end

        # Avoid collision with Kernel#puts / IO#write — name is fine here
        # but note: read/write shadow IO methods if mixed into IO context
        def write(id, data)
          @mutex.synchronize { @pipes[id] << data }
        end

        def read(id)
          @mutex.synchronize do
            arr  = @pipes[id]
            return nil unless arr
            head = @heads[id]
            return nil if head >= arr.size
            item = arr[head]
            arr[head] = nil
            @heads[id] = head + 1
            if head + 1 > 32 && head + 1 > arr.size / 2
              @pipes[id] = arr[(head + 1)..]
              @heads[id] = 0
            end
            item
          end
        end

        def close(id)
          @mutex.synchronize do
            @pipes.delete(id)
            @heads.delete(id)
          end
        end

        def to_a
          @pipes.keys
        end

        def stats
          {
            feature: :ipc_pipe,
            pipes:   @pipes.size
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
  :ipc_pipe,
  __FILE__,
  feature:    :ipc_pipe,
  depends_on: [:ipc]
)
