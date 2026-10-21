# frozen_string_literal: true

# Kestówv 0.5.1 — proc/pid.rb
#
# PID allocation and management.
# Monotonic allocator with a task reference map and recycling support.
# PID 0 is reserved (idle/swapper). PID 1 is init.

module Kestowv
  module Proc
    module Pid

      @next_pid  = 2        # 0 = idle, 1 = init — both reserved
      @pid_map   = {}
      @free_list = []       # recycled PIDs available for reuse
      @mutex     = Mutex.new

      PID_MAX  = 32_768     # matches Linux default pid_max
      RESERVED = [0, 1].freeze

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:proc_pid)
          Boot.set_bit(:proc_pid)

          # Pre-register reserved PIDs so they're never allocated
          RESERVED.each do |pid|
            @pid_map[pid] = { state: :reserved, task: nil }
          end

          self
        end

        # --------------------------------------------------------
        # ALLOCATION
        # --------------------------------------------------------

        # Allocate a PID, optionally binding it to a Task.
        # Prefers recycled PIDs from the free list before monotonic advance.
        # Returns the allocated PID integer, or nil if exhausted.
        def allocate(task: nil)
          pid = @mutex.synchronize do
            p = @free_list.pop || next_pid_unsafe
            return nil unless p

            @pid_map[p] = { state: :allocated, task: task }
            p
          end

          pid
        end

        # Bind an already-allocated PID to a Task after the fact.
        def bind(pid, task)
          @mutex.synchronize do
            entry = @pid_map[pid]
            return false unless entry && entry[:state] == :allocated
            entry[:task] = task
          end
          true
        end

        # Release a PID back to the free list.
        def release(pid)
          return false if pid <= 1

          removed = @mutex.synchronize do
            entry = @pid_map.delete(pid)
            @free_list << pid if entry
            !entry.nil?
          end

          removed
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def get(pid)
          @mutex.synchronize { @pid_map[pid] }
        end

        def task_for(pid)
          @mutex.synchronize { @pid_map[pid]&.[](:task) }
        end

        def allocated?(pid)
          @mutex.synchronize { @pid_map.key?(pid) }
        end

        def all_pids
          @mutex.synchronize { @pid_map.keys.sort }
        end

        def exhausted?
          @mutex.synchronize { @free_list.empty? && @next_pid > PID_MAX }
        end

        # --------------------------------------------------------
        # INTROSPECTION
        # --------------------------------------------------------

        def stats
          @mutex.synchronize do
            by_state = @pid_map.group_by { |_, e| e[:state] }
                                .transform_values(&:count)
            {
              feature:   :proc_pid,
              allocated: @pid_map.size,
              recycled:  @free_list.size,
              next_pid:  @next_pid,
              by_state:  by_state,
              exhausted: @free_list.empty? && @next_pid > PID_MAX
            }
          end
        end

        private

        # Must be called with @mutex held.
        def next_pid_unsafe
          return nil if @next_pid > PID_MAX
          pid = @next_pid
          @next_pid += 1
          pid
        end

      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_pid,
  __FILE__,
  feature:    :proc_pid,
  depends_on: [:proc_task]
)
