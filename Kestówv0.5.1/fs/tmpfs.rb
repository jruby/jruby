# frozen_string_literal: true

# Kestówv 0.5.1 — fs/tmpfs.rb
#
# Pure in-memory filesystem — sovereign baseline for VFS.
# Zero host dependencies. Always available regardless of host OS.
# Supports files, directories, metadata, and size limits.

module Kestowv
  module Fs
    class TmpFs

      # Default max total size — 64MB
      DEFAULT_MAX_BYTES = 64 * 1024 * 1024

      # --------------------------------------------------------
      # NODE — file or directory entry
      # --------------------------------------------------------

      Node = Struct.new(
        :path,
        :kind,        # :file or :dir
        :data,        # String (binary) or nil for dirs
        :size,        # Integer bytes
        :mode,        # protection flags (Mm::Protection bitmask)
        :created_at,
        :modified_at,
        keyword_init: true
      ) do
        def file? = kind == :file
        def dir?  = kind == :dir

        def byte_kind
          kind == :file ? :vfs_file : :vfs_dir
        end

        def to_h
          {
            path:        path,
            kind:        kind,
            size:        size,
            mode:        mode,
            created_at:  created_at,
            modified_at: modified_at
          }
        end
      end

      attr_reader :name, :max_bytes

      # --------------------------------------------------------
      # LIFECYCLE
      # --------------------------------------------------------

      def initialize(name: "tmpfs", max_bytes: DEFAULT_MAX_BYTES)
        @name      = name
        @max_bytes = max_bytes
        @nodes     = {}
        @mutex     = Mutex.new

        # Root directory always exists
        mknode("/", :dir)
      end

      # --------------------------------------------------------
      # DIRECTORY OPS
      # --------------------------------------------------------

      def mkdir(path)
        normalized = normalize(path)
        @mutex.synchronize do
          return false if @nodes.key?(normalized)
          ensure_parent_unsafe(normalized)
          mknode_unsafe(normalized, :dir)
          true
        end
      end

      def rmdir(path)
        normalized = normalize(path)
        @mutex.synchronize do
          node = @nodes[normalized]
          return false unless node&.dir?
          return false if children_unsafe(normalized).any?
          @nodes.delete(normalized)
          true
        end
      end

      def ls(path = "/")
        normalized = normalize(path)
        @mutex.synchronize do
          node = @nodes[normalized]
          return nil unless node&.dir?
          children_unsafe(normalized).map { |n| ::File.basename(n.path) }
        end
      end

      # --------------------------------------------------------
      # FILE OPS
      # --------------------------------------------------------

      def read(path)
        @mutex.synchronize { @nodes[normalize(path)]&.data }
      end

      def write(path, data)
        normalized = normalize(path)
        bytes      = data.to_s.b

        @mutex.synchronize do
          existing   = @nodes[normalized]
          old_size   = existing&.size || 0
          delta      = bytes.bytesize - old_size

          if delta > 0 && used_bytes_unsafe + delta > @max_bytes
            raise StorageFullError, "TmpFs #{@name} full (#{@max_bytes} bytes)"
          end

          ensure_parent_unsafe(normalized)

          if existing
            existing.data        = bytes
            existing.size        = bytes.bytesize
            existing.modified_at = Time.now.freeze
          else
            mknode_unsafe(normalized, :file, data: bytes)
          end
          true
        end
      end

      def delete(path)
        normalized = normalize(path)
        @mutex.synchronize do
          node = @nodes.delete(normalized)
          !node.nil?
        end
      end

      def exists?(path)
        @mutex.synchronize { @nodes.key?(normalize(path)) }
      end

      def stat(path)
        @mutex.synchronize { @nodes[normalize(path)]&.to_h }
      end

      # --------------------------------------------------------
      # BYTE_KIND — VFS integration
      # --------------------------------------------------------

      def byte_kind
        :vfs_mount
      end

      def byte_kind_for(path)
        node = @mutex.synchronize { @nodes[normalize(path)] }
        node&.byte_kind || :vfs_node
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def used_bytes
        @mutex.synchronize { used_bytes_unsafe }
      end

      def file_count
        @mutex.synchronize { @nodes.count { |_, n| n.file? } }
      end

      def dir_count
        @mutex.synchronize { @nodes.count { |_, n| n.dir? } }
      end

      def to_a
        @mutex.synchronize { @nodes.values.map(&:to_h) }
      end

      def stats
        @mutex.synchronize do
          {
            name:       @name,
            max_bytes:  @max_bytes,
            used_bytes: used_bytes_unsafe,
            files:      @nodes.count { |_, n| n.file? },
            dirs:       @nodes.count { |_, n| n.dir? }
          }
        end
      end

      def to_s
        "#<TmpFs #{@name} #{used_bytes}/#{@max_bytes} bytes>"
      end

      # --------------------------------------------------------
      # ERRORS
      # --------------------------------------------------------

      class StorageFullError  < RuntimeError; end
      class NotFoundError     < RuntimeError; end
      class NotDirectoryError < RuntimeError; end

      private

      # --------------------------------------------------------
      # UNSAFE — must be called with @mutex held
      # --------------------------------------------------------

      def mknode(path, kind, data: nil)
        @mutex.synchronize { mknode_unsafe(path, kind, data: data) }
      end

      def mknode_unsafe(path, kind, data: nil)
        bytes = data&.b || "".b
        @nodes[path] = Node.new(
          path:        path,
          kind:        kind,
          data:        kind == :file ? bytes : nil,
          size:        kind == :file ? bytes.bytesize : 0,
          mode:        Mm::Protection::USER_RW,
          created_at:  Time.now.freeze,
          modified_at: Time.now.freeze
        )
      end

      def ensure_parent_unsafe(path)
        parent = ::File.dirname(path)
        return if parent == path   # root
        return if @nodes.key?(parent)
        ensure_parent_unsafe(parent)
        mknode_unsafe(parent, :dir)
      end

      def children_unsafe(dir_path)
        prefix = dir_path == "/" ? "/" : "#{dir_path}/"
        @nodes.values.select do |n|
          n.path != dir_path &&
            n.path.start_with?(prefix) &&
            n.path.delete_prefix(prefix).exclude?("/")
        end
      end

      def used_bytes_unsafe
        @nodes.values.sum(&:size)
      end

      def normalize(p)
        p = p.to_s.strip
        p = "/#{p}" unless p.start_with?("/")
        p = p.gsub(%r{/+}, "/")
        p = p.chomp("/") unless p == "/"
        p
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :fs_tmpfs,
  __FILE__,
  feature:    :fs_tmpfs,
  depends_on: [:fs_vfs, :mm_protection]
)
