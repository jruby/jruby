# frozen_string_literal: true

# Kestówv 0.5.1 - net/raw_socket.rb
#
# Raw socket.
# Registers raw socket features.

module Kestowv
  module Net
    module RawSocket
      @sockets = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:net_raw_socket)
          Boot.set_bit(:net_raw_socket)
        end

        def create(protocol)
          @mutex.synchronize do
            id = SecureRandom.hex(4)
            @sockets[id] = { protocol: protocol }
            id
          end
        end

        def close(id)
          @mutex.synchronize { @sockets.delete(id) }
        end

        def to_a
          @sockets.keys
        end

        def stats
          {
            feature: :net_raw_socket,
            count:   @sockets.size
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
  :net_raw_socket,
  __FILE__,
  feature:    :net_raw_socket,
  depends_on: [:net_socket]
)
