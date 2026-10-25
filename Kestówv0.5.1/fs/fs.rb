# frozen_string_literal: true

# Kestówv 0.5.1 — fs/fs.rb
#
# Top-level Filesystem orchestrator (canonical location inside fs/).
# Initializes VFS, detects host, and mounts the sovereign tmpfs root.
# All fs/ submodules are already accessible as Kestowv::Fs::* —
# this file wires them together and exposes a clean boot interface.

module Kestowv
  module Fs

    @initialized = false
    @mutex       = Mutex.new
    @detection   = nil

    class << self

      # --------------------------------------------------------
      # BOOT REGISTRATION
      # --------------------------------------------------------

      def register_with_boot
        Boot.register(:fs)
        Boot.set_bit(:fs)
        self
      end

      # --------------------------------------------------------
      # INITIALIZATION
      # --------------------------------------------------------

      # Boot sequence:
      #   1. Detect host environment
      #   2. Mount sovereign tmpfs at /
      #   3. Optionally mount hostfs at /host if host is available
      #
      # ns_id: namespace scope for all mounts (nil = root/global)
      def init(ns_id: nil, mount_host: true)
        @mutex.synchronize do
          raise "Fs already initialized — call reset! first" if @initialized

          # Step 1: detect host
          @detection = HostDetector.detect
          warn "[Fs] Detected: #{@detection}" unless Boot.config.quiet

          # Step 2: sovereign tmpfs at root — always available
          root_tmpfs = TmpFs.new(name: "root", max_bytes: 128 * 1024 * 1024)
          Vfs.mount("/", root_tmpfs, ns_id: ns_id)
          warn "[Fs] Mounted tmpfs at /" unless Boot.config.quiet

          # Step 3: host filesystem at /host — only if host is known and detected
          if mount_host && @detection.known? && !@detection.containerized
            host = HostFs.new(detection: @detection, writable: false, root: "/")
            Vfs.mount("/host", host, ns_id: ns_id, flags: Vfs::Flags::RDONLY)
            warn "[Fs] Mounted hostfs at /host (#{@detection.os}, read-only)" unless Boot.config.quiet
          end

          @initialized = true
        end

        Boot.set_bit(:fs_initialized)
        self
      end

      def initialized?
        @mutex.synchronize { @initialized }
      end

      def reset!
        @mutex.synchronize do
          @initialized = false
          @detection   = nil
        end
        Boot.clear_bit(:fs_initialized)
        self
      end

      # --------------------------------------------------------
      # FACTORY INTERFACE
      # --------------------------------------------------------

      # Create a new TmpFs instance (not automatically mounted).
      def tmpfs(name: "tmpfs", max_bytes: TmpFs::DEFAULT_MAX_BYTES)
        TmpFs.new(name: name, max_bytes: max_bytes)
      end

      # Create a new HostFs instance using the cached detection.
      def hostfs(writable: false, root: "/")
        HostFs.new(detection: detection, writable: writable, root: root)
      end

      # Mount a backend at a VFS path.
      def mount(path, backend, ns_id: nil, flags: 0)
        Vfs.mount(path, backend, ns_id: ns_id, flags: flags)
      end

      def unmount(path)
        Vfs.unmount(path)
      end

      # --------------------------------------------------------
      # VFS DELEGATION — convenience passthrough
      # --------------------------------------------------------

      def read(path, ns_id: nil)
        result = Vfs.resolve(path, ns_id: ns_id)
        result&.[](:backend)&.read(result[:relative_path])
      end

      def write(path, data, ns_id: nil)
        result = Vfs.resolve(path, ns_id: ns_id)
        result&.[](:backend)&.write(result[:relative_path], data)
      end

      def exists?(path, ns_id: nil)
        result = Vfs.resolve(path, ns_id: ns_id)
        result&.[](:backend)&.exists?(result[:relative_path]) || false
      end

      def stat(path, ns_id: nil)
        result = Vfs.resolve(path, ns_id: ns_id)
        result&.[](:backend)&.stat(result[:relative_path])
      end

      # --------------------------------------------------------
      # DETECTION
      # --------------------------------------------------------

      def detection
        @mutex.synchronize { @detection ||= HostDetector.detect }
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def stats
        {
          feature:      :fs,
          initialized:  @initialized,
          detection:    detection.to_s,
          vfs:          Vfs.stats,
          host_detector: HostDetector.stats
        }
      end

      def to_s
        "#<Kestowv::Fs initialized=#{@initialized} mounts=#{Vfs.mount_count}>"
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :fs,
  __FILE__,
  feature:    :fs,
  depends_on: [:fs_vfs, :fs_host_detector, :fs_tmpfs, :fs_hostfs]
)
