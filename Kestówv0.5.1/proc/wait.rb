# frozen_string_literal: true

# Kestówv 0.5.1 — proc/wait.rb
#
# Process wait / synchronization.
# Implements waitpid-style semantics against the Task + Pid registries.
# Uses Ruby ConditionVariable for blocking wait without busy-polling.

module Kestowv
  module Proc
    module Wait

      # Wait result — returned to caller on completion.
      Result = Struct.new(:pid, :status, :exit_code, :timed_out, keyword_init: true) do
        def success? = status == :exited && exit_code == 0
        def timed_out? = timed_out
      end

      # WNOHANG — return immediately if no child has exited.
      WNOHANG = 0b01

      @waiters = {}   # pid => Array of ConditionVariable
      @mutex   = Mutex.new
      @stats   = Hash.new(0)

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:proc_wait)
          Boot.set_bit(:proc_wait)
          self
        end

        # --------------------------------------------------------
        # WAIT INTERFACE
        # --------------------------------------------------------

        # Wait for a specific PID to exit.
        # options: bitmask — WNOHANG returns immediately if not yet zombie.
        # timeout: seconds to wait (nil = wait forever).
        #
        # Returns a Result or nil on WNOHANG miss.
        def waitpid(pid, options = 0, timeout: nil)
          task = Pid.task_for(pid)

          unless task
            warn "[Wait] waitpid: unknown PID #{pid}" unless Boot.config.quiet
            return Result.new(pid: pid, status: :no_such_process, exit_code: -1, timed_out: false)
          end

          # Already zombie — collect immediately
          if task.zombie?
            return collect(pid, task)
          end

          # WNOHANG — don't block
          if options & WNOHANG != 0
            return nil
          end

          # Block until the task transitions to zombie
          block_until_zombie(pid, task, timeout: timeout)
        end

        # Wait for any child task to exit (waitpid(-1) semantics).
        # Returns the first Result available, or nil on WNOHANG.
        def wait(options = 0, timeout: nil)
          zombies = find_zombies
          return collect_first(zombies) unless zombies.empty?
          return nil if options & WNOHANG != 0

          # Block on all known PIDs — first zombie wins
          all_pids = Pid.all_pids
          all_pids.each do |pid|
            result = waitpid(pid, options, timeout: timeout)
            return result if result
          end

          nil
        end

        # Called by Task or Scheduler when a task transitions to :zombie.
        # Wakes any threads blocked in waitpid for this PID.
        def notify_exit(pid)
          @mutex.synchronize do
            waiters = @waiters.delete(pid) || []
            waiters.each(&:signal)
          end
          @stats[:notified] += 1
        end

        # --------------------------------------------------------
        # INTROSPECTION
        # --------------------------------------------------------

        def stats
          @mutex.synchronize do
            {
              feature:  :proc_wait,
              waiters:  @waiters.size,
              notified: @stats[:notified],
              collected: @stats[:collected],
              timeouts:  @stats[:timeouts]
            }
          end
        end

        private

        # --------------------------------------------------------
        # BLOCKING WAIT
        # --------------------------------------------------------

        def block_until_zombie(pid, task, timeout:)
          cv = ConditionVariable.new

          @mutex.synchronize do
            @waiters[pid] ||= []
            @waiters[pid] << cv

            deadline = timeout ? Time.now + timeout : nil

            loop do
              break if task.zombie?

              remaining = deadline ? [deadline - Time.now, 0].max : nil
              cv.wait(@mutex, remaining)

              if deadline && Time.now >= deadline
                @waiters[pid]&.delete(cv)
                @stats[:timeouts] += 1
                return Result.new(pid: pid, status: :timeout, exit_code: -1, timed_out: true)
              end
            end
          end

          collect(pid, task)
        end

        # --------------------------------------------------------
        # COLLECTION
        # --------------------------------------------------------

        def collect(pid, task)
          Pid.release(pid)
          @stats[:collected] += 1

          Result.new(
            pid:       pid,
            status:    :exited,
            exit_code: 0,   # TODO: real exit code when Task carries one
            timed_out: false
          )
        end

        def collect_first(zombies)
          pid, task = zombies.first
          collect(pid, task)
        end

        def find_zombies
          Pid.all_pids.each_with_object({}) do |pid, h|
            task = Pid.task_for(pid)
            h[pid] = task if task&.zombie?
          end
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_wait,
  __FILE__,
  feature:    :proc_wait,
  depends_on: [:proc_pid, :proc_task]
)
