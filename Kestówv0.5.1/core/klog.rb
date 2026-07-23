# frozen_string_literal: true

# Kestówv 0.5.1 — core/klog.rb
#
# Kernel logging service.
# Ring buffer with levels, structured output, and Boot integration.

module Kestowv
  module Core
    module Klog

      # Pre-allocated ring buffer — each slot is reused in-place so the
      # hot log() path allocates only a Time object, not a full Hash entry.
      Entry = Struct.new(:time, :level, :message, :context)

      MAX_SIZE  = 1024
      @ring     = Array.new(MAX_SIZE) { Entry.new(nil, nil, nil, nil) }
      @ring_pos = 0       # next write slot (wraps mod MAX_SIZE)
      @filled   = 0       # valid entries written so far (0..MAX_SIZE)
      @level    = :info
      @mutex    = Mutex.new

      LEVELS = {
        debug: 0,
        info:  1,
        warn:  2,
        error: 3
      }.freeze

      class << self

        def register
          Boot.register(:klog)
          Boot.set_bit(:klog)
        end

        def level=(new_level)
          @mutex.synchronize { @level = new_level.to_sym }
        end

        def level
          @mutex.synchronize { @level }
        end

        def log(level, message, **context)
          lvl = level.to_sym
          return if LEVELS[lvl] < LEVELS[@level]

          @mutex.synchronize do
            e          = @ring[@ring_pos]
            e.time     = ::Time.now
            e.level    = lvl
            e.message  = message
            e.context  = context
            @ring_pos  = (@ring_pos + 1) % MAX_SIZE
            @filled    = MAX_SIZE if (@filled += 1) > MAX_SIZE
          end

          unless Boot.config.quiet
            prefix = lvl.to_s.upcase.ljust(5)
            puts "[KLOG] #{prefix} #{message}#{context.empty? ? '' : ' ' + context.inspect}"
          end
        end

        def debug(msg, **ctx) = log(:debug, msg, **ctx)
        def info(msg,  **ctx) = log(:info,  msg, **ctx)
        def warn(msg,  **ctx) = log(:warn,  msg, **ctx)
        def error(msg, **ctx) = log(:error, msg, **ctx)

        def recent(count = 50)
          @mutex.synchronize { ring_ordered.last(count) }
        end

        def clear
          @mutex.synchronize { @filled = 0; @ring_pos = 0 }
        end

        def to_a
          @mutex.synchronize { ring_ordered }
        end

        def stats
          @mutex.synchronize do
            {
              size:     @filled,
              max_size: MAX_SIZE,
              level:    @level,
              feature:  :klog
            }
          end
        end

        private

        # Returns entry structs in insertion order (oldest → newest).
        # Called inside @mutex. Dups each Entry so callers get a stable snapshot.
        def ring_ordered
          if @filled < MAX_SIZE
            @ring[0...@filled].map(&:dup)
          else
            pos = @ring_pos
            (@ring[pos..] + @ring[0...pos]).map(&:dup)
          end
        end
      end
    end
  end
end

Kestowv::Config::Modules.register(
  :core_klog,
  __FILE__,
  feature: :klog
)
