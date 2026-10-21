# frozen_string_literal: true

# Kestówv 0.5.1 - fs/buffer.rb
#
# Buffer cache.
# Registers buffer features.

module Kestowv
  module Fs
    module Buffer
      @buffers = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_buffer)
          Boot.set_bit(:fs_buffer)
        end

        def get(block)
          @mutex.synchronize { @buffers[block] }
        end

        def put(block, data)
          @mutex.synchronize { @buffers[block] = data }
        end

        def to_a
          @buffers.keys
        end

        def stats
          {
            feature: :fs_buffer,
            count:   @buffers.size
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
  :fs_buffer,
  __FILE__,
  feature:    :fs_buffer,
  depends_on: [:fs]
)
