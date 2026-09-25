# frozen_string_literal: true

# Kestówv 0.5.1 — mm/vm_space.rb
#
# Virtual address space.
# Owns a PageTable and a sorted collection of VmRegions.
# One VmSpace per process / address space context.

module Kestowv
  module Mm
    class VmSpace

      attr_reader :page_table

      def initialize(name: nil)
        @name       = name || :"vmspace_#{object_id}"
        @page_table = PageTable.new
        @regions    = []   # sorted by start_vpn for fast overlap checks
        @mutex      = Mutex.new
      end

      # --------------------------------------------------------
      # REGION MANAGEMENT
      # --------------------------------------------------------

      # Add a VmRegion. Raises if it overlaps an existing region.
      def add_region(region)
        raise ArgumentError, "Expected VmRegion" unless region.is_a?(VmRegion)

        @mutex.synchronize do
          conflict = @regions.find { |r| r.overlaps?(region) }
          raise ArgumentError,
            "Region #{region.name} overlaps existing #{conflict.name}" if conflict

          @regions << region
          @regions.sort_by!(&:start_vpn)
        end

        self
      end

      # Remove by identity or by VPN containment.
      def remove_region(region)
        removed = @mutex.synchronize { @regions.delete(region) }
        !removed.nil?
      end

      def remove_region_at(vpn)
        region = find_region(vpn)
        return false unless region
        remove_region(region)
      end

      # --------------------------------------------------------
      # LOOKUP
      # --------------------------------------------------------

      # Binary search since @regions is sorted by start_vpn.
      def find_region(vpn)
        @mutex.synchronize { find_region_unsafe(vpn) }
      end

      def regions
        @mutex.synchronize { @regions.dup }
      end

      def region_count
        @mutex.synchronize { @regions.size }
      end

      # --------------------------------------------------------
      # MAPPING — delegates to PageTable
      # --------------------------------------------------------

      def map(vpn, page, flags: Protection::RW)
        region = find_region(vpn)
        unless region
          raise ArgumentError, "VPN #{vpn} not covered by any region — add a VmRegion first"
        end

        @page_table.map(vpn, page, flags: flags)
      end

      def unmap(vpn)
        @page_table.unmap(vpn)
      end

      def lookup(vpn)
        @page_table.lookup(vpn)
      end

      # --------------------------------------------------------
      # FAULT INTEGRATION
      # --------------------------------------------------------

      # Called by Fault#handle — returns VmSpace as the vm_space arg.
      def handle_fault(vpn, access_type, thread: nil)
        Fault.handle(
          vpn,
          access_type,
          thread:     thread,
          page_table: @page_table,
          vm_space:   self
        )
      end

      # Duck-type compatibility with Fault#find_region's Array/Space check.
      def find(vpn)
        find_region(vpn)
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def to_a
        @mutex.synchronize do
          @regions.map(&:to_h)
        end
      end

      def stats
        pt_stats = @page_table.stats
        @mutex.synchronize do
          {
            name:    @name,
            regions: @regions.size,
            mapped:  pt_stats[:mapped],
            dirty:   pt_stats[:dirty],
            accessed: pt_stats[:accessed]
          }
        end
      end

      def to_s
        "#<VmSpace #{@name} regions=#{region_count} mapped=#{@page_table.stats[:mapped]}>"
      end

      private

      # Must be called with @mutex held.
      # Linear scan for now — could binary search since @regions is sorted.
      def find_region_unsafe(vpn)
        @regions.find { |r| r.contains?(vpn) }
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :mm_vm_space,
  __FILE__,
  feature:    :mm_vm_space,
  depends_on: [:mm_page_table, :mm_vm_region, :mm_fault]
)
