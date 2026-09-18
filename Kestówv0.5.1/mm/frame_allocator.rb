# frozen_string_literal: true

# Kestówv 0.5.1 — mm/frame_allocator.rb
#
# Physical frame allocator.
# Manages pools of Page objects with a free list and allocated map.
# Page state transitions happen outside @mutex to avoid lock inversion
# with Page's own @mutex (inherited from KObject).

module Kestowv
  module Mm
    module FrameAllocator

      @free_pages = []
      @allocated  = {}
      @mutex      = Mutex.new
      @total      = 0

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:mm_frame_allocator)
          Boot.set_bit(:mm_frame_allocator)
          self
        end

        # --------------------------------------------------------
        # INITIALIZATION
        # --------------------------------------------------------

        # Seed the allocator with total_pages free frames.
        # Safe to call multiple times — clears existing state first.
        def init(total_pages, page_size: 4096)
          raise ArgumentError, "total_pages must be positive" unless total_pages.is_a?(Integer) && total_pages > 0

          pages = total_pages.times.map { |i| Page.new(pfn: i, size: page_size, state: :free) }

          @mutex.synchronize do
            # Release any previously allocated pages
            @allocated.each_value { |p| p.release }

            @free_pages.clear
            @allocated.clear
            @free_pages.concat(pages)
            @total = total_pages
          end

          Boot.set_bit(:mm_frame_allocator)
          self
        end

        # --------------------------------------------------------
        # ALLOCATION
        # --------------------------------------------------------

        # Allocate one free page.
        # Returns the Page or nil if pool is exhausted.
        def allocate
          # Step 1: pull from free list under lock
          page = @mutex.synchronize { @free_pages.pop }
          return nil unless page

          # Step 2: transition page state OUTSIDE @mutex
          # Page#allocate! uses Page's own @mutex — holding FrameAllocator's
          # @mutex here too would create a lock-order inversion with #free.
          page.allocate!

          # Step 3: register in allocated map under lock
          @mutex.synchronize { @allocated[page.pfn] = page }

          page
        end

        # Allocate n contiguous pages by PFN.
        # Returns array of Pages or nil if contiguous run unavailable.
        def allocate_contiguous(n)
          pages = @mutex.synchronize do
            run = find_contiguous_unsafe(n)
            return nil unless run
            run.each { |p| @free_pages.delete(p) }
            run
          end

          pages.each(&:allocate!)
          @mutex.synchronize { pages.each { |p| @allocated[p.pfn] = p } }
          pages
        end

        # --------------------------------------------------------
        # DEALLOCATION
        # --------------------------------------------------------

        # Return a page to the free pool.
        def free(page)
          return false unless page

          # Step 1: remove from allocated map under lock
          removed = @mutex.synchronize { @allocated.delete(page.pfn) }
          return false unless removed

          # Step 2: transition state OUTSIDE @mutex — same lock-order reason
          page.free!

          # Step 3: return to free list under lock
          @mutex.synchronize { @free_pages << page }

          true
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def free_count
          @mutex.synchronize { @free_pages.size }
        end

        def allocated_count
          @mutex.synchronize { @allocated.size }
        end

        def get_allocated(pfn)
          @mutex.synchronize { @allocated[pfn] }
        end

        def exhausted?
          @mutex.synchronize { @free_pages.empty? }
        end

        def utilization
          return 0.0 if @total.zero?
          allocated_count.to_f / @total
        end

        def stats
          @mutex.synchronize do
            {
              feature:     :mm_frame_allocator,
              total:       @total,
              free:        @free_pages.size,
              allocated:   @allocated.size,
              utilization: @total.zero? ? 0.0 : (@allocated.size.to_f / @total).round(4)
            }
          end
        end

        private

        # Must be called with @mutex held.
        # Finds n contiguous pages by PFN using a sliding window.
        def find_contiguous_unsafe(n)
          sorted = @free_pages.sort_by(&:pfn)

          sorted.each_cons(n) do |run|
            pfns = run.map(&:pfn)
            return run if pfns.each_cons(2).all? { |a, b| b == a + 1 }
          end

          nil
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :mm_frame_allocator,
  __FILE__,
  feature:    :mm_frame_allocator,
  depends_on: [:mm_page, :hal_memory]
)
