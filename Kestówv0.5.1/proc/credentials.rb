# frozen_string_literal: true

# Kestówv 0.5.1 — proc/credentials.rb
#
# Process credentials: UID/GID sets and capability bitmask.
# Mirrors Linux's cred struct — real/effective/saved IDs + capability sets.
# Credentials are immutable once created; use derive to produce new ones.

module Kestowv
  module Proc
    module Credentials

      # --------------------------------------------------------
      # CAPABILITIES — subset of Linux capability constants
      # --------------------------------------------------------

      module Cap
        CHOWN        = 0   # make arbitrary changes to file UIDs/GIDs
        DAC_OVERRIDE = 1   # bypass file read/write/exec permission checks
        KILL         = 5   # send signals to any process
        SETUID       = 7   # make arbitrary changes to process UIDs
        SETGID       = 8   # make arbitrary changes to process GIDs
        NET_ADMIN    = 12  # perform network administration tasks
        SYS_ADMIN    = 21  # range of system administration operations
        SYS_PTRACE   = 19  # trace arbitrary processes

        ALL = [CHOWN, DAC_OVERRIDE, KILL, SETUID, SETGID,
               NET_ADMIN, SYS_ADMIN, SYS_PTRACE].freeze

        def self.name_for(cap)
          constants.find { |c| const_get(c) == cap && c != :ALL }
        end
      end

      # --------------------------------------------------------
      # CRED — immutable credential set
      # --------------------------------------------------------

      Cred = Struct.new(
        :uid, :euid, :suid,    # real / effective / saved user IDs
        :gid, :egid, :sgid,    # real / effective / saved group IDs
        :groups,               # supplementary group list
        :caps_permitted,       # capability bitmask — what the process may use
        :caps_effective,       # capability bitmask — what the process is using
        keyword_init: true
      ) do
        def root?
          euid == 0
        end

        def member?(gid)
          self.gid == gid || egid == gid || groups.include?(gid)
        end

        def capable?(cap)
          caps_effective & (1 << cap) != 0
        end

        def permitted?(cap)
          caps_permitted & (1 << cap) != 0
        end

        # Raise a capability from permitted to effective.
        # Returns a new Cred — originals are immutable.
        def raise_cap(cap)
          return self unless permitted?(cap)
          derive(caps_effective: caps_effective | (1 << cap))
        end

        # Drop a capability from effective set.
        def drop_cap(cap)
          derive(caps_effective: caps_effective & ~(1 << cap))
        end

        # Produce a new Cred with overridden fields.
        def derive(**overrides)
          Cred.new(**to_h.merge(overrides))
        end

        def to_s
          "Cred(uid=#{uid}/#{euid} gid=#{gid}/#{egid} caps=#{caps_string})"
        end

        # Slab support — reset to safe zeroed defaults (called by Pool#acquire).
        def slab_reclaim!
          self.uid = self.euid = self.suid = 0
          self.gid = self.egid = self.sgid = 0
          self.groups         = [].freeze
          self.caps_permitted = 0
          self.caps_effective = 0
          self
        end

        # Update fields after pool acquisition — called by Credentials.slab_acquire.
        def slab_reuse!(uid:, gid:, groups:, cap_mask:)
          self.uid            = uid
          self.euid           = uid
          self.suid           = uid
          self.gid            = gid
          self.egid           = gid
          self.sgid           = gid
          self.groups         = groups
          self.caps_permitted = cap_mask
          self.caps_effective = cap_mask
          self
        end

        # Return this Cred to the slab pool instead of letting it be GC'd.
        def slab_release
          Credentials.cred_pool&.release(self)
          self
        end

        private

        def caps_string
          Cap::ALL.select { |c| capable?(c) }
                  .map   { |c| Cap.name_for(c)&.to_s&.downcase || c.to_s }
                  .join(",")
                  .then  { |s| s.empty? ? "none" : s }
        end
      end

      # --------------------------------------------------------
      # MODULE INTERFACE
      # --------------------------------------------------------

      CRED_POOL_CAPACITY = 64

      @cred_count = 0
      @cred_pool  = nil
      @mutex      = Mutex.new

      class << self

        def register_with_boot
          Boot.register(:proc_credentials)
          Boot.set_bit(:proc_credentials)
          self
        end

        # Pre-allocate a pool of reusable Cred structs.
        # Called during MM slab pool init (after Mm::Slab is loaded).
        def init_pool(capacity: CRED_POOL_CAPACITY)
          @mutex.synchronize do
            @cred_pool = Mm::Slab.create_pool(:credentials, capacity: capacity) do
              Cred.new(uid: 0, euid: 0, suid: 0,
                       gid: 0, egid: 0, sgid: 0,
                       groups: [].freeze,
                       caps_permitted: 0, caps_effective: 0)
            end
          end
        end

        def cred_pool
          @mutex.synchronize { @cred_pool }
        end

        # Acquire a Cred from the slab pool (or allocate if pool is exhausted).
        # Caller MUST call cred.slab_release when done to return it to the pool.
        def slab_acquire(uid: 0, gid: 0, groups: [], caps: [])
          cap_mask      = caps.reduce(0) { |m, c| m | (1 << c) }
          frozen_groups = groups.frozen? ? groups : groups.dup.freeze
          cred = @mutex.synchronize { @cred_pool }&.acquire
          if cred
            cred.slab_reuse!(uid: uid, gid: gid, groups: frozen_groups, cap_mask: cap_mask)
          else
            create(uid: uid, gid: gid, groups: groups, caps: caps)
          end
        end

        # Create a new immutable Cred (always allocates — use slab_acquire in hot paths).
        def create(uid: 0, gid: 0, groups: [], caps: [])
          cap_mask = caps.reduce(0) { |m, c| m | (1 << c) }

          cred = Cred.new(
            uid:            uid,
            euid:           uid,
            suid:           uid,
            gid:            gid,
            egid:           gid,
            sgid:           gid,
            groups:         groups.dup.freeze,
            caps_permitted: cap_mask,
            caps_effective: cap_mask
          )

          @mutex.synchronize { @cred_count += 1 }
          cred
        end

        # Root credential with all capabilities.
        def root
          create(uid: 0, gid: 0, caps: Cap::ALL)
        end

        # Unprivileged user credential — no capabilities.
        def user(uid:, gid:, groups: [])
          create(uid: uid, gid: gid, groups: groups, caps: [])
        end

        # Check if a cred passes a permission check for a given
        # protection flag set (integrates with mm::Protection).
        def permitted_access?(cred, flags)
          return true if cred.root?
          return true if cred.capable?(Cap::DAC_OVERRIDE)

          prot = Mm::Protection
          return false if prot.executable?(flags) && !cred.capable?(Cap::SYS_ADMIN)
          true
        end

        def stats
          @mutex.synchronize do
            {
              feature:      :proc_credentials,
              creds_issued: @cred_count,
              pool_stats:   @cred_pool&.stats
            }
          end
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_credentials,
  __FILE__,
  feature:    :proc_credentials,
  depends_on: [:proc_task, :mm_protection]
)
