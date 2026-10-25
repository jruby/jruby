# frozen_string_literal: true

# Kestówv 0.5.1 — fs/vfs.rb
#
# Core Virtual Filesystem.
# Mount table uses the established @mutex + _unsafe pattern.
# Every mount carries a ByteClass kind and ns_id from day one.
# Namespace-aware: resolve checks ns_id before returning a backend.

module Kestowv
  module Fs
    module Vfs

      # VFS node kinds — fed into Boot bit vector and ByteClass system.
      NODE_KINDS = %i[vfs_mount vfs_file vfs_dir vfs_symlink vfs_node].freeze

      # Mount entry — immutable after creation.
      Mount = Struct.new(
        :mount_point,   # normalized String path
        :backend,       # backend object (TmpFs, HostFs, etc.)
        :ns_id,         # IPC namespace ID (nil = root/global)
        :flags,         # mount flags bitmask
        :mounted_at,    # Time
        :byte_kind,     # ByteClass kind symbol
        keyword_init: true
      ) do
        def root?    = mount_point == "/"
        def to_s     = "#<VFS::Mount #{mount_point} backend=#{backend.class} ns=#{ns_id}>"
      end

      # Mount flags
      module Flags
        RDONLY   = 0b0001   # read-only mount
        NOEXEC   = 0b0010   # no exec on this mount
        NOSUID   = 0b0100   # ignore suid bits
        BIND     = 0b1000   # bind mount (alias)
      end

      # --------------------------------------------------------
      # REGISTRY
      # --------------------------------------------------------

      @mounts = {}   # normalized_path => Mount (sorted on insert)
      @mutex  = Mutex.new

      class << self

        # --------------------------------------------------------
        # BOOT
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:fs_vfs)
          Boot.set_bit(:fs_vfs)
          self
        end

        # --------------------------------------------------------
        # MOUNT TABLE
        # --------------------------------------------------------

        def mount(mount_point, backend, ns_id: nil, flags: 0)
          raise ArgumentError, "backend must respond to :read" unless backend.respond_to?(:read)

          normalized = normalize_path(mount_point)

          @mutex.synchronize do
            mount_unsafe(normalized, backend, ns_id: ns_id, flags: flags)
          end

          Boot.set_bit_raw(Boot.register(vfs_bit(normalized)))
          self
        end

        def unmount(mount_point)
          normalized = normalize_path(mount_point)

          removed = @mutex.synchronize { unmount_unsafe(normalized) }
          if removed
            pos = Boot.bit_position(vfs_bit(normalized))
            Boot.clear_bit_raw(pos) if pos
          end
          !removed.nil?
        end

        # --------------------------------------------------------
        # PATH RESOLUTION
        # --------------------------------------------------------

        # Resolve a path to its mount entry.
        # ns_id: if provided, only return mounts visible in that namespace.
        # Returns a Hash with :mount, :backend, :relative_path or nil.
        def resolve(path, ns_id: nil)
          normalized = normalize_path(path)
          @mutex.synchronize { resolve_unsafe(normalized, ns_id: ns_id) }
        end

        # Convenience — resolve and return just the backend.
        def backend_for(path, ns_id: nil)
          result = resolve(path, ns_id: ns_id)
          result&.[](:backend)
        end

        # --------------------------------------------------------
        # NAMESPACE CHECK
        # --------------------------------------------------------

        # Can a task with the given ns_id see this mount?
        # nil ns_id on mount = globally visible.
        # nil ns_id on request = root namespace = sees everything.
        def visible?(mount, requesting_ns_id)
          return true if mount.ns_id.nil?         # global mount
          return true if requesting_ns_id.nil?    # root requester
          mount.ns_id == requesting_ns_id
        end

        # --------------------------------------------------------
        # BYTECLASS INTEGRATION
        # --------------------------------------------------------

        def byte_kind_for(type)
          case type.to_sym
          when :mount   then :vfs_mount
          when :file    then :vfs_file
          when :dir     then :vfs_dir
          when :symlink then :vfs_symlink
          else               :vfs_node
          end
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def mounts(ns_id: nil)
          @mutex.synchronize do
            entries = @mounts.values
            ns_id ? entries.select { |m| visible?(m, ns_id) } : entries
          end
        end

        def mounted?(mount_point)
          @mutex.synchronize { @mounts.key?(normalize_path(mount_point)) }
        end

        def mount_count
          @mutex.synchronize { @mounts.size }
        end

        def to_a
          @mutex.synchronize { @mounts.values.map(&:to_s) }
        end

        def stats
          @mutex.synchronize do
            by_ns = @mounts.values.group_by(&:ns_id).transform_values(&:count)
            {
              feature:     :fs_vfs,
              mounts:      @mounts.size,
              by_namespace: by_ns
            }
          end
        end

        private

        # --------------------------------------------------------
        # UNSAFE — must be called with @mutex held
        # --------------------------------------------------------

        def mount_unsafe(normalized, backend, ns_id:, flags:)
          @mounts[normalized] = Mount.new(
            mount_point: normalized,
            backend:     backend,
            ns_id:       ns_id,
            flags:       flags,
            mounted_at:  Time.now.freeze,
            byte_kind:   :vfs_mount
          )
          # Keep sorted longest-first for correct prefix resolution
          @mounts = @mounts.sort_by { |k, _| -k.length }.to_h
        end

        def unmount_unsafe(normalized)
          @mounts.delete(normalized)
        end

        def resolve_unsafe(normalized, ns_id:)
          @mounts.each do |mp, mount|
            next unless normalized == mp || normalized.start_with?("#{mp}/") || mp == "/"
            next unless visible?(mount, ns_id)

            relative = normalized.delete_prefix(mp)
            relative = "/#{relative}" unless relative.start_with?("/")
            relative = "/" if relative.empty?

            return {
              mount:         mount,
              backend:       mount.backend,
              mount_point:   mp,
              relative_path: relative,
              ns_id:         mount.ns_id,
              flags:         mount.flags
            }
          end
          nil
        end

        def normalize_path(p)
          p = p.to_s.strip
          p = "/#{p}" unless p.start_with?("/")
          # Collapse double slashes, strip trailing slash (except root)
          p = p.gsub(%r{/+}, "/")
          p = p.chomp("/") unless p == "/"
          p
        end

        def vfs_bit(normalized_path)
          safe = normalized_path.gsub("/", "_").gsub(/[^a-z0-9_]/, "")
          :"vfs_mount#{safe}"
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :fs_vfs,
  __FILE__,
  feature:    :fs,
  depends_on: [:proc_namespace, :ipc_namespace]
)
