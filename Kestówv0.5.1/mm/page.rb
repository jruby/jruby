# frozen_string_literal: true

# Kestówv 0.5.1 — mm/page.rb
#
# Physical page frame abstraction.
# Represents one physical memory page (default 4KB).
# Inherits identity, refcount, and lifecycle from KObject.

module Kestowv
  module Mm
    class Page < Kestowv::Core::KObject

      attr_reader :pfn, :size

      # Page Frame states — mirrors Linux page lifecycle.
      STATES = %i[free allocated reserved].freeze

      def initialize(pfn:, size: 4096, state: :free)
        raise ArgumentError, "Unknown page state: #{state}" unless STATES.include?(state)
        raise ArgumentError, "pfn must be non-negative Integer" unless pfn.is_a?(Integer) && pfn >= 0

        super(type_tag: :page)

        @pfn        = pfn
        @size       = size
        @state      = state
        @dirty      = false
        @referenced = false
      end

      # --------------------------------------------------------
      # STATE MANAGEMENT
      # All mutations go through @mutex (inherited from KObject).
      # --------------------------------------------------------

      def allocate!
        @mutex.synchronize do
          next false unless @state == :free
          @state      = :allocated
          @dirty      = false
          @referenced = true
          true
        end
      end

      def free!
        @mutex.synchronize do
          next false unless @state == :allocated
          @state      = :free
          @dirty      = false
          @referenced = false
          true
        end
      end

      def reserve!
        @mutex.synchronize do
          next false if @state == :reserved
          @state = :reserved
          true
        end
      end

      def mark_dirty
        @mutex.synchronize { @dirty = true }
        self
      end

      def mark_referenced
        @mutex.synchronize { @referenced = true }
        self
      end

      def clear_referenced
        @mutex.synchronize { @referenced = false }
        self
      end

      # --------------------------------------------------------
      # QUERY — all reads through @mutex for consistency
      # --------------------------------------------------------

      def state
        @mutex.synchronize { @state }
      end

      def dirty?
        @mutex.synchronize { @dirty }
      end

      def referenced?
        @mutex.synchronize { @referenced }
      end

      def free?
        @mutex.synchronize { @state == :free }
      end

      def allocated?
        @mutex.synchronize { @state == :allocated }
      end

      def reserved?
        @mutex.synchronize { @state == :reserved }
      end

      # --------------------------------------------------------
      # LIFECYCLE OVERRIDE
      # --------------------------------------------------------

      def destroy
        @mutex.synchronize do
          @state      = :free
          @dirty      = false
          @referenced = false
        end
        super
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def to_h
        @mutex.synchronize do
          super.merge(
            pfn:        @pfn,
            size:       @size,
            state:      @state,
            dirty:      @dirty,
            referenced: @referenced
          )
        end
      end

      def to_s
        "#<Page pfn=#{@pfn} state=#{state} size=#{@size} rc=#{refcount}>"
      end

    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :mm_page,
  __FILE__,
  feature:    :mm_page,
  depends_on: [:core_kobject, :hal_memory]
)
