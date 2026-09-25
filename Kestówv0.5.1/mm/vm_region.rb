# frozen_string_literal: true

# Kestówv 0.5.1 — mm/vm_region.rb
#
# Virtual Memory Region (VMA).
# Represents a contiguous virtual address range with protection flags
# and optional backing store. Mirrors Linux's vm_area_struct concept.

module Kestowv
  module Mm
    class VmRegion < Kestowv::Core::KObject

      attr_reader :start_vpn, :num_pages, :name

      # Protection flags — mirrors POSIX mmap prot values.
      module Prot
        NONE  = 0b0000
        READ  = 0b0001
        WRITE = 0b0010
        EXEC  = 0b0100

        def self.to_s(flags)
          parts = []
          parts << "r" if flags & READ  != 0
          parts << "w" if flags & WRITE != 0
          parts << "x" if flags & EXEC  != 0
          parts.empty? ? "---" : parts.join
        end
      end

      # Backing store types.
      BACKING       = %i[anonymous file device shared].freeze
      POOL_CAPACITY = 256

      # --------------------------------------------------------
      # SLAB POOL — class-level object recycler
      # --------------------------------------------------------

      @pool         = nil
      @pool_mu      = Mutex.new
      @in_pool_init = false

      class << self
        def init_pool(capacity: POOL_CAPACITY)
          @pool_mu.synchronize do
            return if @pool
            @in_pool_init = true
            @pool = Mm::Slab.create_pool(:vm_region, capacity: capacity) do
              new(start_vpn: 0, num_pages: 1, backing: :anonymous, name: :_slab)
            end
            @in_pool_init = false
          end
          self
        end

        def slab_pool
          @pool
        end

        # Acquire from pool or fall back to heap allocation.
        def slab_acquire(start_vpn:, num_pages:, flags: Prot::READ, name: nil, backing: :anonymous)
          obj = @pool&.acquire
          if obj
            obj.reuse!(start_vpn: start_vpn, num_pages: num_pages,
                       flags: flags, name: name, backing: backing)
          else
            new(start_vpn: start_vpn, num_pages: num_pages,
                flags: flags, name: name, backing: backing)
          end
        end
      end

      def initialize(start_vpn:, num_pages:, flags: Prot::READ, name: nil, backing: :anonymous)
        raise ArgumentError, "start_vpn must be non-negative"  unless start_vpn.is_a?(Integer) && start_vpn >= 0
        raise ArgumentError, "num_pages must be positive"      unless num_pages.is_a?(Integer)  && num_pages > 0
        raise ArgumentError, "Unknown backing type: #{backing}" unless BACKING.include?(backing)

        super(type_tag: :vm_region)

        @start_vpn = start_vpn
        @num_pages = num_pages
        @flags     = flags
        @name      = name || :"vma_#{id}"
        @backing   = backing
      end

      # --------------------------------------------------------
      # RANGE QUERIES — all reads through @mutex
      # --------------------------------------------------------

      def flags
        @mutex.synchronize { @flags }
      end

      def backing
        @mutex.synchronize { @backing }
      end

      def end_vpn
        @start_vpn + @num_pages - 1
      end

      def vpn_range
        @start_vpn..end_vpn
      end

      def contains?(vpn)
        vpn >= @start_vpn && vpn <= end_vpn
      end

      def overlaps?(other_region)
        start_vpn <= other_region.end_vpn &&
          other_region.start_vpn <= end_vpn
      end

      def size_bytes(page_size: 4096)
        @num_pages * page_size
      end

      # --------------------------------------------------------
      # PROTECTION
      # --------------------------------------------------------

      def readable?
        @mutex.synchronize { @flags & Prot::READ  != 0 }
      end

      def writable?
        @mutex.synchronize { @flags & Prot::WRITE != 0 }
      end

      def executable?
        @mutex.synchronize { @flags & Prot::EXEC  != 0 }
      end

      def protect(new_flags)
        @mutex.synchronize { @flags = new_flags }
        self
      end

      def prot_string
        Prot.to_s(flags)
      end

      # --------------------------------------------------------
      # SLAB REUSE — reinitialise payload after pool acquire
      # --------------------------------------------------------

      def reuse!(start_vpn:, num_pages:, flags: Prot::READ, name: nil, backing: :anonymous)
        raise ArgumentError, "start_vpn must be non-negative" unless start_vpn >= 0
        raise ArgumentError, "num_pages must be positive"     unless num_pages > 0
        raise ArgumentError, "Unknown backing: #{backing}"    unless BACKING.include?(backing)
        @mutex.synchronize do
          @start_vpn = start_vpn
          @num_pages = num_pages
          @flags     = flags
          @name      = name || :"vma_#{@id}"
          @backing   = backing
        end
        self
      end

      # Return to slab pool instead of letting GC reclaim.
      def slab_release
        @mutex.synchronize do
          @refcount -= 1
          return self if @refcount > 0
          @destroyed = true
        end
        self.class.slab_pool&.release(self)
        self
      end

      # --------------------------------------------------------
      # LIFECYCLE OVERRIDE
      # --------------------------------------------------------

      def destroy
        super
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def to_h
        @mutex.synchronize do
          super.merge(
            start_vpn:  @start_vpn,
            end_vpn:    end_vpn,
            num_pages:  @num_pages,
            flags:      @flags,
            prot:       Prot.to_s(@flags),
            name:       @name,
            backing:    @backing
          )
        end
      end

      def to_s
        "#<VmRegion #{@name} vpn=#{@start_vpn}..#{end_vpn} prot=#{prot_string} rc=#{refcount}>"
      end

    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :mm_vm_region,
  __FILE__,
  feature:    :mm_vm_region,
  depends_on: [:mm_page_table]
)
