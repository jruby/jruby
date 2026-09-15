# frozen_string_literal: true

# Kestówv 0.5.1 — fs/hostfs.rb
#
# Host filesystem adapter.
# Routes I/O through platform-appropriate paths based on HostDetector.
# Guards all operations against detection capabilities — never assumes
# a host feature is available without confirming it first.
#
# Read-only by default. Write access requires explicit opt-in.

module Kestowv
  module Fs
    class HostFs

      # --------------------------------------------------------
      # PLATFORM ADAPTERS — selected by HostDetector at init
      # --------------------------------------------------------

      module Posix
        def self.read(path, mode: "rb")
          ::File.binread(path)
        rescue Errno::ENOENT, Errno::EACCES => e
          raise HostFs::AccessError, e.message
        end

        def self.write(path, data)
          ::File.binwrite(path, data)
        rescue Errno::EACCES => e
          raise HostFs::AccessError, e.message
        end

        def self.stat(path)
          s = ::File.stat(path)
          {
            path:     path,
            size:     s.size,
            mode:     s.mode,
            kind:     s.directory? ? :dir : :file,
            mtime:    s.mtime.freeze,
            readable: s.readable?,
            writable: s.writable?
          }
        rescue Errno::ENOENT
          nil
        end

        def self.ls(path)
          Dir.entries(path).reject { |e| e == "." || e == ".." }
        rescue Errno::ENOENT, Errno::ENOTDIR
          nil
        end

        def self.mkdir(path)
          Dir.mkdir(path)
          true
        rescue Errno::EEXIST
          true
        rescue Errno::EACCES => e
          raise HostFs::AccessError, e.message
        end

        def self.delete(path)
          ::File.delete(path)
          true
        rescue Errno::ENOENT
          false
        rescue Errno::EACCES => e
          raise HostFs::AccessError, e.message
        end

        def self.exists?(path)
          ::File.exist?(path)
        end
      end

      module Windows
        # Windows paths need separator normalization.
        def self.normalize(path)
          path.to_s.gsub("/", "\\")
        end

        def self.read(path, **)
          ::File.binread(normalize(path))
        rescue Errno::ENOENT, Errno::EACCES => e
          raise HostFs::AccessError, e.message
        end

        def self.write(path, data)
          ::File.binwrite(normalize(path), data)
        rescue Errno::EACCES => e
          raise HostFs::AccessError, e.message
        end

        def self.stat(path)
          Posix.stat(normalize(path))   # Ruby's ::File.stat works on Windows
        end

        def self.ls(path)
          Posix.ls(normalize(path))
        end

        def self.mkdir(path)
          Posix.mkdir(normalize(path))
        end

        def self.delete(path)
          Posix.delete(normalize(path))
        end

        def self.exists?(path)
          ::File.exist?(normalize(path))
        end
      end

      # --------------------------------------------------------
      # LIFECYCLE
      # --------------------------------------------------------

      def initialize(detection: nil, writable: false, root: "/")
        @detection = detection || HostDetector.detect
        @writable  = writable
        @root      = root   # chroot-style prefix applied to all paths
        @adapter   = select_adapter
        @mutex     = Mutex.new
        @stats     = { reads: 0, writes: 0, errors: 0 }
      end

      # --------------------------------------------------------
      # BACKEND INTERFACE (matches TmpFs contract)
      # --------------------------------------------------------

      def read(path)
        guard_capability!(:posix)
        full = full_path(path)

        result = @adapter.read(full)
        @mutex.synchronize { @stats[:reads] += 1 }
        result
      rescue AccessError => e
        @mutex.synchronize { @stats[:errors] += 1 }
        Boot.handle_error(e, { path: full, op: :read })
        nil
      end

      def write(path, data)
        guard_writable!
        guard_capability!(:posix)
        full = full_path(path)

        @adapter.write(full, data)
        @mutex.synchronize { @stats[:writes] += 1 }
        true
      rescue AccessError => e
        @mutex.synchronize { @stats[:errors] += 1 }
        Boot.handle_error(e, { path: full, op: :write })
        false
      end

      def exists?(path)
        @adapter.exists?(full_path(path))
      rescue
        false
      end

      def stat(path)
        @adapter.stat(full_path(path))
      rescue
        nil
      end

      def ls(path = "/")
        @adapter.ls(full_path(path))
      rescue
        nil
      end

      def mkdir(path)
        guard_writable!
        @adapter.mkdir(full_path(path))
      end

      def delete(path)
        guard_writable!
        @adapter.delete(full_path(path))
      end

      # --------------------------------------------------------
      # BYTECLASS INTEGRATION
      # --------------------------------------------------------

      def byte_kind
        :vfs_mount
      end

      def byte_kind_for(path)
        s = stat(path)
        return :vfs_node unless s
        s[:kind] == :dir ? :vfs_dir : :vfs_file
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def writable?
        @writable
      end

      def stats
        @mutex.synchronize do
          {
            feature:      :fs_hostfs,
            os:           @detection&.os,
            byte_kind:    @detection&.byte_kind,
            containerized: @detection&.containerized,
            capabilities: @detection&.capabilities&.to_a,
            root:         @root,
            writable:     @writable,
            adapter:      @adapter.name,
            stats:        @stats.dup
          }
        end
      end

      def to_s
        "#<HostFs os=#{@detection&.os} root=#{@root} writable=#{@writable}>"
      end

      # --------------------------------------------------------
      # ERRORS
      # --------------------------------------------------------

      class AccessError   < RuntimeError; end
      class ReadOnlyError < RuntimeError; end
      class NotAvailableError < RuntimeError; end

      private

      def select_adapter
        return Windows if @detection&.windows?
        Posix
      end

      def full_path(path)
        p = path.to_s
        p = p.delete_prefix("/") if @root != "/"
        ::File.join(@root, p)
      end

      def guard_writable!
        raise ReadOnlyError, "HostFs mounted read-only" unless @writable
      end

      def guard_capability!(cap)
        return if @detection.nil?
        unless @detection.capabilities.include?(cap)
          raise NotAvailableError,
            "Host capability :#{cap} not available on #{@detection.os}"
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :fs_hostfs,
  __FILE__,
  feature:    :fs_hostfs,
  depends_on: [:fs_vfs, :fs_host_detector]
)
