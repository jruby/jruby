# frozen_string_literal: true

# Kestówv 0.5.1 — core/kthread.rb
#
# Kernel thread management with KObject integration and scheduler awareness.
# Wraps Ruby ::Thread with TID allocation, lifecycle tracking, and Boot bit vector.
#
# NOTE: Named KThread to avoid shadowing ::Thread inside this scope.

module Kestowv
  module Core
    module KThread

      @threads  = {}
      @next_tid = 0
      @mutex    = Mutex.new

      # Thread states mirror standard kernel thread states.
      STATES = %i[running sleeping stopped zombie].freeze

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:core_thread)
          Boot.set_bit(:core_thread)
          self
        end

        # --------------------------------------------------------
        # THREAD LIFECYCLE
        # --------------------------------------------------------

        def create(name: nil, priority: :normal, &block)
          raise ArgumentError, "Block required for KThread.create" unless block

          tid     = allocate_tid
          kobj    = Kestowv::Core::KObject.new(type_tag: :thread)
          tname   = name || :"thread_#{tid}"
          bit_pos = Boot.register(:"thread_#{tid}")   # once per thread lifetime

          # Build the Ruby thread — capture tid/kobj/bit_pos before any mutex re-entry.
          ruby_thread = ::Thread.new do
            ::Thread.current.name = tname.to_s
            block.call
          rescue => e
            Boot.handle_error(e, { tid: tid, thread_name: tname })
          ensure
            # Transition to zombie on natural exit so join can detect completion.
            transition_state(tid, :zombie)
            Boot.clear_bit_raw(bit_pos)
            kobj.release
          end

          entry = {
            kobject:    kobj,
            thread:     ruby_thread,
            name:       tname,
            state:      :running,
            priority:   priority,
            created_at: ::Time.now.freeze,
            bit_pos:    bit_pos
          }

          @mutex.synchronize { @threads[tid] = entry }
          Boot.set_bit_raw(bit_pos)

          tid
        end

        # Extract the Ruby thread under lock, then join OUTSIDE the lock.
        # Joining inside @mutex deadlocks if the thread calls any KThread
        # method during teardown (e.g. transition_state in ensure block).
        def join(tid, timeout: nil)
          ruby_thread = @mutex.synchronize { @threads[tid]&.[](:thread) }
          return false unless ruby_thread

          ruby_thread.join(timeout)
          true
        end

        def kill(tid)
          entry = @mutex.synchronize { @threads.delete(tid) }
          return false unless entry

          entry[:thread].kill
          entry[:kobject].release
          Boot.clear_bit_raw(entry[:bit_pos])
          true
        end

        def sleep_thread(tid)
          transition_state(tid, :sleeping)
        end

        def wake_thread(tid)
          transition_state(tid, :running)
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def state(tid)
          @mutex.synchronize { @threads[tid]&.[](:state) }
        end

        def alive?(tid)
          entry = @mutex.synchronize { @threads[tid] }
          entry && entry[:thread].alive?
        end

        def active
          @mutex.synchronize { @threads.keys.dup }
        end

        def to_a
          @mutex.synchronize do
            @threads.map do |tid, entry|
              {
                tid:        tid,
                name:       entry[:name],
                state:      entry[:state],
                priority:   entry[:priority],
                created_at: entry[:created_at],
                alive:      entry[:thread].alive?
              }
            end.sort_by { |e| e[:tid] }
          end
        end

        def stats
          @mutex.synchronize do
            by_state = STATES.each_with_object({}) do |s, h|
              h[s] = @threads.count { |_, e| e[:state] == s }
            end

            {
              feature:  :core_thread,
              total:    @threads.size,
              by_state: by_state
            }
          end
        end

        private

        # Atomic TID allocation — increment and return in one lock.
        def allocate_tid
          @mutex.synchronize { @next_tid += 1 }
        end

        # Safe state transition — no-op if tid no longer registered.
        def transition_state(tid, new_state)
          @mutex.synchronize do
            entry = @threads[tid]
            entry[:state] = new_state if entry
          end
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# depends_on :core_scheduler removed until that module exists.
# Add it back when scheduler.rb lands.
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :core_thread,
  __FILE__,
  feature:    :thread,
  depends_on: [:core_kobject, :core_syscall]
)
