# frozen_string_literal: true

# Kestówv 0.5.1 — mm/protection.rb
#
# Unified memory protection bits.
# Single source of truth — used by VmRegion, PageTable::Entry,
# and any future mm subsystem that needs prot flags.
#
# NOTE: VmRegion had a local Prot module — replace it with this.
#       PageTable::Flags maps onto these same constants.

module Kestowv
  module Mm
    module Protection

      # --------------------------------------------------------
      # CORE PROTECTION BITS — matches POSIX mmap + x86-64 PTE
      # --------------------------------------------------------

      NONE  = 0b00000000
      READ  = 0b00000001
      WRITE = 0b00000010
      EXEC  = 0b00000100

      # --------------------------------------------------------
      # PRIVILEGE BITS
      # --------------------------------------------------------

      USER   = 0b00001000   # accessible from user space
      KERNEL = 0b00010000   # kernel-only (implied when USER is clear)

      # --------------------------------------------------------
      # SOFTWARE-MAINTAINED TRACKING BITS
      # --------------------------------------------------------

      DIRTY    = 0b00100000   # page has been written since last clear
      ACCESSED = 0b01000000   # page has been read or written recently
      SHARED   = 0b10000000   # mapping is shared (copy-on-write semantics)

      # --------------------------------------------------------
      # COMMON COMBINATIONS
      # --------------------------------------------------------

      RO        = READ
      RW        = READ  | WRITE
      RX        = READ  | EXEC
      RWX       = READ  | WRITE | EXEC

      USER_RO   = USER  | READ
      USER_RW   = USER  | READ  | WRITE
      USER_RX   = USER  | READ  | EXEC
      USER_RWX  = USER  | READ  | WRITE | EXEC

      KERNEL_RO = KERNEL | READ
      KERNEL_RW = KERNEL | READ  | WRITE
      KERNEL_RX = KERNEL | READ  | EXEC

      # --------------------------------------------------------
      # ALL VALID BITS MASK — use for validation
      # --------------------------------------------------------

      ALL_BITS = NONE | READ | WRITE | EXEC | USER | KERNEL |
                 DIRTY | ACCESSED | SHARED

      # --------------------------------------------------------
      # PREDICATES
      # --------------------------------------------------------

      def self.readable?(f)    = (f & READ)     != 0
      def self.writable?(f)    = (f & WRITE)    != 0
      def self.executable?(f)  = (f & EXEC)     != 0
      def self.user?(f)        = (f & USER)     != 0
      def self.kernel?(f)      = (f & USER)     == 0
      def self.dirty?(f)       = (f & DIRTY)    != 0
      def self.accessed?(f)    = (f & ACCESSED) != 0
      def self.shared?(f)      = (f & SHARED)   != 0

      # --------------------------------------------------------
      # MANIPULATION HELPERS
      # --------------------------------------------------------

      def self.set(flags, bit)    = flags | bit
      def self.clear(flags, bit)  = flags & ~bit
      def self.toggle(flags, bit) = flags ^ bit

      def self.mark_dirty(flags)     = set(flags, DIRTY)
      def self.mark_accessed(flags)  = set(flags, ACCESSED)
      def self.clear_dirty(flags)    = clear(flags, DIRTY)
      def self.clear_accessed(flags) = clear(flags, ACCESSED)

      # Validate flags — returns true if no unknown bits are set.
      def self.valid?(flags)
        (flags & ~ALL_BITS) == 0
      end

      # --------------------------------------------------------
      # DISPLAY
      # --------------------------------------------------------

      def self.to_s(flags)
        return "---" if flags == NONE

        [
          [READ,     "r"],
          [WRITE,    "w"],
          [EXEC,     "x"],
          [USER,     "u"],
          [DIRTY,    "d"],
          [ACCESSED, "a"],
          [SHARED,   "s"]
        ].each_with_object(+'') do |(bit, char), s|
          s << (flags & bit != 0 ? char : '-')
        end
      end

      def self.inspect(flags)
        names = {
          READ     => :READ,
          WRITE    => :WRITE,
          EXEC     => :EXEC,
          USER     => :USER,
          KERNEL   => :KERNEL,
          DIRTY    => :DIRTY,
          ACCESSED => :ACCESSED,
          SHARED   => :SHARED
        }

        active = names.select { |bit, _| flags & bit != 0 }.values
        "Protection(#{active.join(' | ')})"
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :mm_protection,
  __FILE__,
  feature:    :mm_protection,
  depends_on: []
)
