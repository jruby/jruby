# frozen_string_literal: true

# Kestówv 0.5.1 - net/socket.rb
#
# Socket abstraction.
# Registers socket features.

module Kestowv
  module Net
    module Socket
      @sockets = {}
      @next_fd  = 3
      @mutex    = Mutex.new

      class << self
        def register_features
          Boot.register(:net_socket)
          Boot.set_bit(:net_socket)
        end

        def create(domain, type, protocol)
          @mutex.synchronize do
            fd = @next_fd
            @next_fd += 1
            @sockets[fd] = { domain: domain, type: type, protocol: protocol, state: :closed }
            fd
          end
        end

        def close(fd)
          @mutex.synchronize { @sockets.delete(fd) }
        end

        def to_a
          @sockets.keys
        end

        def stats
          {
            feature: :net_socket,
            open:    @sockets.size
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
  :net_socket,
  __FILE__,
  feature:    :net_socket,
  depends_on: [:net_device]
)
