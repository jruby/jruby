# frozen_string_literal: true

# Kestówv 0.5.1 - ipc/sem.rb
#
# Semaphore (updated).
# Registers semaphore features.

module Kestowv
  module Ipc
    module Sem
      @sems  = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:ipc_sem)
          Boot.set_bit(:ipc_sem)
        end

        def create(key, value)
          @mutex.synchronize { @sems[key] = value }
        end

        def wait(key)
          @mutex.synchronize do
            @sems[key] -= 1 if @sems[key] && @sems[key] > 0
          end
        end

        def post(key)
          @mutex.synchronize { @sems[key] += 1 if @sems[key] }
        end

        def to_a
          @sems
        end

        def stats
          {
            feature:    :ipc_sem,
            semaphores: @sems.size
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
  :ipc_sem,
  __FILE__,
  feature:    :ipc_sem,
  depends_on: [:ipc]
)
