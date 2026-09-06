# frozen_string_literal: true

# Kestówv 0.5.1 — hal/interrupts.rb
#
# Interrupt handling abstraction with classification and stats.

module Kestowv
  module Hal
    module Interrupts

      @handlers = {}
      @stats    = Hash.new(0)
      @mutex    = Mutex.new

      IRQ_TYPES = {
        timer:     0,
        keyboard:  1,
        network:   2,
        storage:   3,
        ipi:       4
      }.freeze

      class << self

        def register
          Boot.register(:hal_interrupts)
          Boot.set_bit(:hal_interrupts)
        end

        def register_irq(irq, type: :ipi, &block)
          @mutex.synchronize do
            @handlers[irq] = { handler: block, type: type }
          end
        end

        def handle(irq)
          entry = @mutex.synchronize { @handlers[irq] }
          return false unless entry

          @mutex.synchronize { @stats[irq] += 1 }

          begin
            entry[:handler].call if entry[:handler]
            true
          rescue => e
            Boot.handle_error(e, { irq: irq, type: entry[:type] })
            false
          end
        end

        def stats
          @mutex.synchronize do
            {
              feature:   :hal_interrupts,
              handlers:  @handlers.size,
              delivered: @stats.values.sum
            }
          end
        end

        def to_a
          @mutex.synchronize { @handlers.keys.sort }
        end
      end
    end
  end
end

Kestowv::Config::Modules.register(
  :hal_interrupts,
  __FILE__,
  feature:    :hal_interrupts,
  depends_on: [:hal_cpu]
)
