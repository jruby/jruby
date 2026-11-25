# frozen_string_literal: true

# Kestówv 0.5.1 - net/unix_socket.rb
#
# Unix domain socket abstraction.
# Uses MessagePack for default serialization.
# JSON is only for explicit human debugging.

require 'msgpack'

module Kestowv
  module Net
    module UnixSocket
      @sockets = {}
      @mutex   = Mutex.new

      class << self
        def register_features
          Boot.register(:net_unix_socket)
          Boot.set_bit(:net_unix_socket)
        end

        # Create and connect a client UNIX socket
        def new(path, type: :stream)
          register_features
          socket = self.new_internal(path, type)
          UnixHub.register(socket)
          socket
        end

        # Create an unnamed connected pair
        def pair(type: :stream)
          register_features
          s1 = self.new_internal("::pair::1", type)
          s2 = self.new_internal("::pair::2", type)
          # In real impl these would be connected
          UnixHub.register(s1)
          UnixHub.register(s2)
          [s1, s2]
        end

        alias socketpair pair

        # Internal constructor (not exposed directly)
        def new_internal(path, type)
          @mutex.synchronize do
            socket = Socket.new(path, type)
            @sockets[path] = socket
            socket
          end
        end

        def close(path)
          @mutex.synchronize do
            socket = @sockets.delete(path)
            UnixHub.unregister(path) if socket
            socket&.close
          end
        end

        def to_a
          @sockets.keys
        end

        def stats
          {
            feature: :net_unix_socket,
            active:  @sockets.size
          }
        end
      end

      # Instance class for individual sockets
      class Socket
        attr_reader :path, :type

        def initialize(path, type = :stream)
          @path  = path
          @type  = type
          @state = :closed
          @stats = { bytes_sent: 0, bytes_received: 0 }
        end

        def connect
          @state = :connected
          true
        end

        def close
          @state = :closed
          true
        end

        def closed?
          @state == :closed
        end

        # Send IO with optional metadata (MessagePack by default)
        def send_io(io, metadata = nil)
          serialized = metadata ? MessagePack.pack(metadata) : nil
          # In real implementation this would use SCM_RIGHTS
          { io: io, metadata: serialized }
        end

        # Receive IO (returns metadata decoded from MessagePack)
        def recv_io
          # Placeholder - real impl would receive fd + metadata
          { io: nil, metadata: nil }
        end

        def to_a
          {
            path:  @path,
            type:  @type,
            state: @state
          }
        end

        def stats
          @stats.merge(feature: :net_unix_socket)
        end

        # Human debugging only
        def to_debug_json
          JSON.pretty_generate(to_a)
        end
      end
    end
  end
end
# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :net_unix_socket,
  __FILE__,
  feature:    :net_unix_socket,
  depends_on: [:net_unix_hub, :net_socket]
)
