# frozen_string_literal: true

# Kestówv 0.5.1 - fs/mount.rb
#
# Mount point management.
# Registers mount features.

module Kestowv
  module Fs
    module Mount
      @mounts = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_mount)
          Boot.set_bit(:fs_mount)
        end

        def mount(device, path, fs_type)
          @mutex.synchronize do
            @mounts[path] = { device: device, fs_type: fs_type }
          end
        end

        def unmount(path)
          @mutex.synchronize { @mounts.delete(path) }
        end

        def to_a
          @mounts
        end

        def stats
          {
            feature: :fs_mount,
            mounts:  @mounts.size
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
  :fs_mount,
  __FILE__,
  feature:    :fs_mount,
  depends_on: [:fs]
)
