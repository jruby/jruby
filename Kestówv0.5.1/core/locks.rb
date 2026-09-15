# frozen_string_literal: true

# Kestówv 0.5.1 — core/locks.rb
#
# Locking primitives with contention tracking and KThread awareness.

module Kestowv
  module Core
    module Locks

      @locks = {}
      @mutex = Mutex.new

      class << self

        def register
          Boot.register(:core_locks)
          Boot.set_bit(:core_locks)
        end

        def create(name, type: :mutex)
          @mutex.synchronize do
            @locks[name] = {
              type: type,
              mutex: Mutex.new,
              waiters: 0,
              acquisitions: 0
            }
          end
        end

        def synchronize(name)
          lock = @mutex.synchronize { @locks[name] }
          return yield unless lock

          @mutex.synchronize { lock[:waiters] += 1 }
          begin
            lock[:mutex].synchronize do
              @mutex.synchronize { lock[:acquisitions] += 1 }
              yield
            end
          ensure
            @mutex.synchronize { lock[:waiters] -= 1 }
          end
        end

        def stats
          @mutex.synchronize do
            @locks.transform_values do |l|
              {
                type: l[:type],
                waiters: l[:waiters],
                acquisitions: l[:acquisitions]
              }
            end
          end
        end

        def to_a
          @mutex.synchronize { @locks.keys.dup }
        end
      end
    end
  end
end

Kestowv::Config::Modules.register(
  :core_locks,
  __FILE__,
  feature: :locks
)
