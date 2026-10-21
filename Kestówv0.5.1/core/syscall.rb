# frozen_string_literal: true

# Kestówv 0.5.1 — core/syscall.rb
#
# System call interface.
# Registration + dispatch table with Boot integration.
# Syscall numbers mirror Linux x86-64 ABI for familiarity.

module Kestowv
  module Core
    module Syscall

      @handlers    = {}
      @names       = {}
      @call_log    = []     # circular ring buffer
      @call_log_pos = 0     # next write slot
      @mutex       = Mutex.new

      # Linux x86-64 ABI syscall numbers (extend as system grows).
      # Gaps are intentional — unimplemented calls return :enosys.
      SYSCALLS = {
        read:        0,
        write:       1,
        open:        2,
        close:       3,
        getpid:     39,
        sched_yield: 24,
        fork:        57,
        execve:      59,
        exit:        60
      }.freeze

      # Sentinel returned when a syscall number has no handler.
      ENOSYS = :enosys

      class << self

        # --------------------------------------------------------
        # REGISTRATION
        # --------------------------------------------------------

        # Register a syscall handler by name.
        # Number defaults to SYSCALLS[name] if not given.
        # Raises if neither is found — silent gaps are dangerous.
        def register(name, number = nil, &block)
          key = name.to_sym
          num = number || SYSCALLS[key]

          raise ArgumentError, "No syscall number for :#{key} — pass one explicitly" unless num

          @mutex.synchronize do
            @handlers[num] = block if block
            @names[key]    = num

            feat = :"syscall_#{key}"
            Boot.register(feat)
            Boot.set_bit(feat)  # mark as registered in the bit vector
          end

          self
        end

        # Deregister a handler — leaves the name→number mapping intact.
        def deregister(name)
          key = name.to_sym
          @mutex.synchronize do
            num = @names[key]
            return false unless num

            @handlers.delete(num)
            Boot.clear_bit(:"syscall_#{key}")
          end
          true
        end

        # --------------------------------------------------------
        # DISPATCH
        # --------------------------------------------------------

        # Invoke by syscall number.
        # Returns ENOSYS if no handler is registered.
        def invoke(number, *args)
          handler = @mutex.synchronize { @handlers[number] }

          unless handler
            warn "[Syscall] No handler for number #{number}" unless Boot.config.quiet
            return ENOSYS
          end

          log_call(number, args)
          handler.call(*args)
        rescue => e
          Boot.handle_error(e, { syscall: number, args: args })
          ENOSYS
        end

        # Invoke by name, forwarding all args.
        def invoke_named(name, *args)
          num = @mutex.synchronize { @names[name.to_sym] }

          unless num
            warn "[Syscall] Unknown syscall :#{name}" unless Boot.config.quiet
            return ENOSYS
          end

          invoke(num, *args)
        end

        # Convenience alias — reads naturally at call sites.
        alias_method :call, :invoke_named

        # --------------------------------------------------------
        # INTROSPECTION
        # --------------------------------------------------------

        def registered
          @mutex.synchronize { @names.keys.sort }
        end

        def registered?(name)
          @mutex.synchronize { @names.key?(name.to_sym) }
        end

        def handler_for(name)
          num     = @mutex.synchronize { @names[name.to_sym] }
          return nil unless num
          @mutex.synchronize { @handlers[num] }
        end

        def to_a
          @mutex.synchronize do
            @names.map do |name, num|
              {
                name:        name,
                number:      num,
                has_handler: @handlers.key?(num),
                bit_set:     Boot.bit_set?(:"syscall_#{name}")
              }
            end.sort_by { |e| e[:number] }
          end
        end

        def stats
          @mutex.synchronize do
            {
              registered:     @names.size,
              with_handlers:  @handlers.size,
              without_handlers: @names.size - @handlers.size,
              feature:        :core_syscall
            }
          end
        end

        # Recent call log — useful for boot-phase tracing.
        # Capped at 256 entries, returned in insertion order.
        def call_log
          @mutex.synchronize do
            if @call_log.size < LOG_CAP
              @call_log.dup
            else
              pos = @call_log_pos
              @call_log[pos..] + @call_log[0...pos]
            end
          end
        end

        private

        LOG_CAP = 256

        def log_call(number, args)
          return if Boot.config.quiet

          entry = { number: number, args: args, at: ::Time.now }
          @mutex.synchronize do
            if @call_log.size < LOG_CAP
              @call_log << entry
            else
              @call_log[@call_log_pos] = entry   # overwrite oldest — O(1)
            end
            @call_log_pos = (@call_log_pos + 1) % LOG_CAP
          end
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER — depends on KObject being loaded first.
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :core_syscall,
  __FILE__,
  feature:    :syscall,
  depends_on: [:core_kobject]
)
