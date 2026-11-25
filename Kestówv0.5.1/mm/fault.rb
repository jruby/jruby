# frozen_string_literal: true

# Kestówv 0.5.1 — mm/fault.rb
#
# Page fault handler.
# Decides what to do when a memory access cannot be satisfied.
#
# Fault resolution order:
#   1. No region covering VPN        → SEGV
#   2. Region found, no PTE          → demand page (allocate + map)
#   3. PTE present, protection check → protection fault or COW
#   4. PTE present, no violation     → spurious / hardware fault

module Kestowv
  module Mm
    module Fault

      # Fault result codes — callers pattern-match on these.
      RESULTS = %i[resolved segv protection_fault cow oom spurious].freeze

      ACCESS_TYPES = %i[read write exec].freeze

      # Per-fault-type stats
      @stats = Hash.new(0)
      @mutex = Mutex.new

      class << self

        # --------------------------------------------------------
        # ENTRY POINT
        # --------------------------------------------------------

        # Handle a page fault for the given VPN and access type.
        # thread: the KThread::entry hash (or nil if not thread-aware yet)
        # page_table: the active PageTable for this address space
        # vm_space: the VmSpace (or array of VmRegions) for the process
        #
        # Returns a RESULTS symbol.
        def handle(vpn, access_type, thread: nil, page_table: nil, vm_space: nil)
          raise ArgumentError, "Unknown access_type: #{access_type}" unless ACCESS_TYPES.include?(access_type)

          region = find_region(vpn, vm_space)
          unless region
            record(:segv)
            return handle_segv(vpn, access_type, thread: thread)
          end

          entry = page_table&.entry(vpn)
          unless entry
            result = handle_demand_page(vpn, region, access_type, page_table: page_table)
            record(result)
            return result
          end

          if protection_violation?(entry, access_type)
            record(:protection_fault)
            return handle_protection_fault(vpn, access_type, entry, thread: thread)
          end

          # PTE present, no violation — COW candidate or spurious
          result = handle_cow_or_spurious(vpn, entry, access_type, page_table: page_table)
          record(result)
          result
        end

        def stats
          @mutex.synchronize { @stats.dup }
        end

        def reset_stats!
          @mutex.synchronize { @stats.clear }
        end

        private

        # --------------------------------------------------------
        # REGION LOOKUP
        # --------------------------------------------------------

        def find_region(vpn, vm_space)
          return nil unless vm_space

          case vm_space
          when Array
            vm_space.find { |r| r.contains?(vpn) }
          else
            vm_space.find(vpn) rescue nil
          end
        end

        # --------------------------------------------------------
        # PROTECTION CHECK
        # --------------------------------------------------------

        def protection_violation?(entry, access_type)
          flags = entry.flags
          case access_type
          when :read  then !Protection.readable?(flags)
          when :write then !Protection.writable?(flags)
          when :exec  then !Protection.executable?(flags)
          end
        end

        # --------------------------------------------------------
        # FAULT HANDLERS
        # --------------------------------------------------------

        def handle_segv(vpn, access_type, thread:)
          warn "[Fault] SEGV vpn=#{vpn} access=#{access_type}" unless Boot.config.quiet
          # TODO: deliver SIGSEGV to thread when signal system exists
          :segv
        end

        def handle_demand_page(vpn, region, access_type, page_table:)
          return :segv unless page_table

          # Check access is permitted by region protection
          if protection_violation_region?(region, access_type)
            warn "[Fault] Demand page protection denied vpn=#{vpn}" unless Boot.config.quiet
            return :protection_fault
          end

          page = FrameAllocator.allocate
          unless page
            warn "[Fault] OOM on demand page vpn=#{vpn}" unless Boot.config.quiet
            return :oom
          end

          # Derive PTE flags from region protection
          pte_flags = region_to_pte_flags(region)
          page_table.map(vpn, page, flags: pte_flags)

          # Mark accessed — this was a fresh mapping
          page.mark_referenced
          :resolved
        end

        def handle_protection_fault(vpn, access_type, entry, thread:)
          warn "[Fault] Protection fault vpn=#{vpn} access=#{access_type} " \
               "flags=#{Protection.to_s(entry.flags)}" unless Boot.config.quiet
          # TODO: deliver SIGBUS to thread when signal system exists
          :protection_fault
        end

        def handle_cow_or_spurious(vpn, entry, access_type, page_table:)
          # Write to a shared page → COW candidate
          if access_type == :write && Protection.shared?(entry.flags)
            return handle_cow(vpn, entry, page_table: page_table)
          end

          # Otherwise spurious — hardware retried a resolved fault
          warn "[Fault] Spurious fault vpn=#{vpn} access=#{access_type}" unless Boot.config.quiet
          :spurious
        end

        def handle_cow(vpn, entry, page_table:)
          return :spurious unless page_table

          new_page = FrameAllocator.allocate
          return :oom unless new_page

          # Copy old page content — placeholder until real memcpy exists
          # new_page.copy_from(entry.page)

          # Remap with WRITE, clear SHARED
          new_flags = Protection.clear(entry.flags, Protection::SHARED) | Protection::WRITE
          page_table.remap(vpn, new_page, flags: new_flags)

          # Release reference to old shared page
          entry.page.release

          :cow
        end

        # --------------------------------------------------------
        # HELPERS
        # --------------------------------------------------------

        def protection_violation_region?(region, access_type)
          flags = region.flags
          case access_type
          when :read  then !Protection.readable?(flags)
          when :write then !Protection.writable?(flags)
          when :exec  then !Protection.executable?(flags)
          end
        end

        def region_to_pte_flags(region)
          flags  = Protection::NONE
          rflags = region.flags
          flags |= Protection::READ     if Protection.readable?(rflags)
          flags |= Protection::WRITE    if Protection.writable?(rflags)
          flags |= Protection::EXEC     if Protection.executable?(rflags)
          flags |= Protection::USER     if Protection.user?(rflags)
          flags
        end

        def record(result)
          @mutex.synchronize { @stats[result] += 1 }
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :mm_fault,
  __FILE__,
  feature:    :mm_fault,
  depends_on: [:mm_page_table, :mm_vm_region, :mm_frame_allocator, :mm_protection]
)
