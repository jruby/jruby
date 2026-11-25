# frozen_string_literal: true

# Kestówv 0.5.1 - net/udp_socket.rb
#
# UDP socket.
# Registers UDP socket features.

module Kestowv
  module Net
    module UdpSocket
      @sockets = {}
      @mutex   = Mutex.new

      class << self
        def register_features
          Boot.register(:net_udp_socket)
          Boot.set_bit(:net_udp_socket)
        end

        def create
          @mutex.synchronize do
            id = SecureRandom.hex(4)
            @sockets[id] = { local_port: nil }
            id
          end
        end

        def bind(id, port)
          @mutex.synchronize { @sockets[id][:local_port] = port if @sockets[id] }
        end

        def close(id)
          @mutex.synchronize { @sockets.delete(id) }
        end

        def to_a
          @sockets.keys
        end

        def stats
          {
            feature: :net_udp_socket,
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
  :net_udp_socket,
  __FILE__,
  feature:    :net_udp_socket,
  depends_on: [:net_socket]
)
