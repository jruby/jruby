# frozen_string_literal: true

# Kestówv 0.5.1 - mm/heap.rb
#
# Basic heap management simulation.
# Registers heap features.

module Kestowv
  module Mm
    module Heap
      @allocated = 0
      @limit     = 256 * 1024 * 1024  # 256MB default
      @mutex     = Mutex.new

      class << self
        def register_features
          Boot.register(:mm_heap)
          Boot.set_bit(:mm_heap)
        end

        def allocate(size)
          @mutex.synchronize do
            return false unless @allocated + size <= @limit
            @allocated += size
            true
          end
        end

        def free(size)
          @mutex.synchronize do
            @allocated -= size if @allocated >= size
          end
        end

        def usage
          @allocated
        end

        def limit
          @limit
        end

        def to_a
          {
            allocated: @allocated,
            limit:     @limit
          }
        end

        def stats
          {
            feature: :mm_heap,
            usage:   @allocated,
            percent: (@allocated.to_f / @limit * 100).round(2)
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
  :mm_heap,
  __FILE__,
  feature:    :mm_heap,
  depends_on: [:mm_page]
)
