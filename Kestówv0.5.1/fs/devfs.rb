# frozen_string_literal: true

# Kestówv 0.5.1 - fs/devfs.rb
#
# Devfs (device filesystem) simulation.
# Registers devfs features.

module Kestowv
  module Fs
    module Devfs
      @devices = {}
      @mutex   = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_devfs)
          Boot.set_bit(:fs_devfs)
        end

        def register_device(name, type)
          @mutex.synchronize { @devices[name] = { type: type } }
        end

        def to_a
          @devices.keys
        end

        def stats
          {
            feature: :fs_devfs,
            devices: @devices.size
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
  :fs_devfs,
  __FILE__,
  feature:    :fs_devfs,
  depends_on: [:fs]
)
