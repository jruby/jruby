# frozen_string_literal: true

# Kestówv 0.5.1 - fs/super.rb
#
# Superblock management.
# Registers superblock features.

module Kestowv
  module Fs
    module Super
      @superblocks = {}
      @mutex       = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_super)
          Boot.set_bit(:fs_super)
        end

        def create(device, fs_type)
          @mutex.synchronize do
            @superblocks[device] = {
              fs_type:    fs_type,
              mounted_at: Time.now
            }
          end
        end

        def get(device)
          @superblocks[device]
        end

        def to_a
          @superblocks
        end

        def stats
          {
            feature: :fs_super,
            count:   @superblocks.size
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
  :fs_super,
  __FILE__,
  feature:    :fs_super,
  depends_on: [:fs]
)
