# frozen_string_literal: true

# Kestówv 0.5.1 — hal/memory.rb
#
# Memory abstraction (physical + virtual regions).

module Kestowv
  module Hal
    module Memory

      @regions = {}
      @mutex   = Mutex.new

      class << self

        def register
          Boot.register(:hal_memory)
          Boot.set_bit(:hal_memory)
        end
        alias register_with_boot register

        def add_region(name, start, size, type: :ram)
          @mutex.synchronize do
            @regions[name] = {
              name:  name,
              start: start,
              size:  size,
              type:  type,
              used:  0
            }
          end
        end

        def get(name)
          @mutex.synchronize { @regions[name] }
        end

        def total_ram
          @mutex.synchronize do
            @regions.values.select { |r| r[:type] == :ram }.sum { |r| r[:size] }
          end
        end

        def to_a
          @mutex.synchronize { @regions.values.dup }
        end

        def stats
          @mutex.synchronize do
            {
              feature:   :hal_memory,
              regions:   @regions.size,
              total_ram: total_ram
            }
          end
        end
      end
    end
  end
end

Kestowv::Config::Modules.register(
  :hal_memory,
  __FILE__,
  feature:    :hal_memory,
  depends_on: [:hal_cpu]
)
