# frozen_string_literal: true

# Kestówv 0.5.1 — mm/page_table.rb
#
# Page table management.
# Maps virtual page numbers (VPN) to physical Page objects.
# One PageTable per process/address space.

module Kestowv
  module Mm
    class PageTable

      # Page table entry flags — mirrors x86-64 PTE bits.
      module Flags
        PRESENT    = 0b00000001
        WRITABLE   = 0b00000010
        USER       = 0b00000100
        EXECUTABLE = 0b00001000
        DIRTY      = 0b00010000
        ACCESSED   = 0b00100000
      end

      Entry = Struct.new(:page, :flags, keyword_init: true) do
        def present?    = flags & Flags::PRESENT    != 0
        def writable?   = flags & Flags::WRITABLE   != 0
        def user?       = flags & Flags::USER       != 0
        def executable? = flags & Flags::EXECUTABLE != 0
        def dirty?      = flags & Flags::DIRTY      != 0
        def accessed?   = flags & Flags::ACCESSED   != 0
      end

      def initialize
        @entries = {}   # vpn (Integer) => Entry
        @mutex   = Mutex.new
      end

      # --------------------------------------------------------
      # MAPPING
      # --------------------------------------------------------

      # Map a virtual page number to a physical Page.
      # flags defaults to PRESENT | WRITABLE.
      def map(vpn, page, flags: Flags::PRESENT | Flags::WRITABLE)
        raise ArgumentError, "vpn must be Integer"    unless vpn.is_a?(Integer)
        raise ArgumentError, "page must be a Page"    unless page.is_a?(Kestowv::Mm::Page)
        raise ArgumentError, "page must be allocated" unless page.allocated?

        @mutex.synchronize { @entries[vpn] = Entry.new(page: page, flags: flags) }
        true
      end

      def unmap(vpn)
        entry = @mutex.synchronize { @entries.delete(vpn) }
        entry&.page
      end

      # Remap an existing VPN to a new page — atomic swap.
      def remap(vpn, new_page, flags: Flags::PRESENT | Flags::WRITABLE)
        old_entry = @mutex.synchronize do
          old = @entries[vpn]
          @entries[vpn] = Entry.new(page: new_page, flags: flags)
          old
        end
        old_entry&.page
      end

      # --------------------------------------------------------
      # LOOKUP
      # --------------------------------------------------------

      def lookup(vpn)
        entry = @mutex.synchronize { @entries[vpn] }
        return nil unless entry

        # Mark accessed on lookup — feeds LRU/clock replacement
        entry.flags |= Flags::ACCESSED
        entry.page
      end

      def entry(vpn)
        @mutex.synchronize { @entries[vpn] }
      end

      def mapped?(vpn)
        @mutex.synchronize { @entries.key?(vpn) }
      end

      def vpns
        @mutex.synchronize { @entries.keys.sort }
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      # vpn → pfn mapping for serialization / debug.
      def to_h
        @mutex.synchronize do
          @entries.transform_values { |e| e.page.pfn }
        end
      end

      # Full entry snapshot — vpn → { pfn:, flags: } for tracing.
      def to_a
        @mutex.synchronize do
          @entries.map do |vpn, e|
            {
              vpn:        vpn,
              pfn:        e.page.pfn,
              flags:      e.flags,
              present:    e.present?,
              writable:   e.writable?,
              dirty:      e.dirty?,
              accessed:   e.accessed?
            }
          end.sort_by { |row| row[:vpn] }
        end
      end

      def stats
        @mutex.synchronize do
          {
            mapped:    @entries.size,
            writable:  @entries.count { |_, e| e.writable? },
            dirty:     @entries.count { |_, e| e.dirty? },
            accessed:  @entries.count { |_, e| e.accessed? }
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
  :mm_page_table,
  __FILE__,
  feature:    :mm_page_table,
  depends_on: [:mm_page, :mm_frame_allocator]
)
