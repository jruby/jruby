# frozen_string_literal: true

# Kestówv 0.5.1 — mm/slab.rb
#
# Slab allocator.
#
# Two layers:
#   Slab::Pool  — real object pool.  Pre-allocates N instances via a factory
#                 block, hands them out via acquire / release with zero GC
#                 pressure on the hot path.  acquire calls slab_reclaim! on
#                 the returned object so KObject lifecycle fields are reset
#                 before the caller's reuse! method reinitialises payload.
#
#   Slab (module) — named slab registry (accounting) + Pool registry.
#                   Legacy counter-based create_slab/allocate/free kept for
#                   compat; new code uses create_pool / pool.

module Kestowv
  module Mm
    module Slab

      # ----------------------------------------------------------------
      # Pool — zero-GC object recycler
      # ----------------------------------------------------------------
      class Pool
        attr_reader :name, :capacity

        def initialize(name:, capacity:, &factory)
          raise ArgumentError, "Pool requires a factory block" unless factory
          @name      = name
          @capacity  = capacity
          @free      = capacity.times.map { factory.call }
          @acquired  = 0
          @misses    = 0
          @total_req = 0
          @mutex     = Mutex.new
        end

        def acquire
          obj = @mutex.synchronize do
            @total_req += 1
            if @free.empty?
              @misses += 1
              nil
            else
              @acquired += 1
              @free.pop
            end
          end
          # slab_reclaim! outside pool mutex — avoids lock inversion
          # with the object's own @mutex (KObject#slab_reclaim! locks it).
          obj&.slab_reclaim!
          obj
        end

        def release(obj)
          @mutex.synchronize do
            return self if @free.size >= @capacity
            @free.push(obj)
            @acquired -= 1 if @acquired > 0
          end
          self
        end

        def free_count
          @mutex.synchronize { @free.size }
        end

        def hit_rate
          @mutex.synchronize do
            @total_req == 0 ? 1.0 : ((@total_req - @misses).to_f / @total_req).round(4)
          end
        end

        def stats
          @mutex.synchronize do
            { name:      @name,
              capacity:  @capacity,
              free:      @free.size,
              acquired:  @acquired,
              misses:    @misses,
              hit_rate:  @total_req == 0 ? 1.0 :
                           ((@total_req - @misses).to_f / @total_req).round(4) }
          end
        end
      end

      # ----------------------------------------------------------------
      # Module-level pool registry
      # ----------------------------------------------------------------
      @pools      = {}
      @pool_mutex = Mutex.new

      def self.create_pool(name, capacity:, &factory)
        pool = Pool.new(name: name, capacity: capacity, &factory)
        @pool_mutex.synchronize { @pools[name] = pool }
        pool
      end

      def self.pool(name)
        @pool_mutex.synchronize { @pools[name] }
      end

      def self.pool_stats
        @pool_mutex.synchronize { @pools.transform_values(&:stats) }
      end

      # ----------------------------------------------------------------
      # Legacy counter-based named slab API (kept for compat)
      # ----------------------------------------------------------------
      @slabs = {}
      @mutex = Mutex.new

      def self.create_slab(name, object_size, count)
        @mutex.synchronize do
          @slabs[name] = { object_size: object_size, total: count, free: count }
        end
      end

      def self.allocate(name)
        @mutex.synchronize do
          slab = @slabs[name]
          return nil unless slab && slab[:free] > 0
          slab[:free] -= 1
          true
        end
      end

      def self.free(name)
        @mutex.synchronize do
          slab = @slabs[name]
          return false unless slab
          slab[:free] += 1 if slab[:free] < slab[:total]
          true
        end
      end

      def self.to_a
        @slabs
      end

      def self.stats
        pools = @pool_mutex.synchronize { @pools.transform_values(&:stats) }
        { feature: :mm_slab, slabs: @slabs.size, pools: pools }
      end

      def self.register_features
        Boot.register(:mm_slab)
        Boot.set_bit(:mm_slab)
      end

    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :mm_slab,
  __FILE__,
  feature:    :mm_slab,
  depends_on: [:mm_page]
)
