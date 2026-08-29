# frozen_string_literal: true

# Kestówv 0.5.1 - net/unix_hub.rb
#
# Central hub for UNIX domain sockets.
# MessagePack is default for internal serialization.
# JSON is only for explicit human debugging.

require 'msgpack'

module Kestowv
  module Net
    module UnixHub
      @sockets = {}
      @mutex   = Mutex.new
      @backend = :jdk   # :jdk, :jnr, or :ruby

      class << self
        def register_features
          Boot.register(:net_unix_hub)
          Boot.set_bit(:net_unix_hub)
        end

        def register(socket)
          register_features
          @mutex.synchronize do
            @sockets[socket.path] = socket
          end
          true
        end

        def unregister(path_or_socket)
          key = path_or_socket.is_a?(String) ? path_or_socket : path_or_socket.path
          @mutex.synchronize { @sockets.delete(key) }
          true
        end

        def get(path)
          @mutex.synchronize { @sockets[path] }
        end

        def list
          @mutex.synchronize { @sockets.keys }
        end

        def active_sockets
          @mutex.synchronize { @sockets.values }
        end

        def cleanup
          count = 0
          @mutex.synchronize do
            @sockets.delete_if do |_path, sock|
              if sock.closed?
                count += 1
                true
              else
                false
              end
            end
          end
          count
        end

        def backend=(name)
          @backend = name.to_sym
        end

        def current_backend
          @backend
        end

        def to_a
          {
            active: list.size,
            backend: @backend,
            sockets: list
          }
        end

        def stats
          {
            feature: :net_unix_hub,
            active_sockets: list.size,
            backend: @backend
          }
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
  :net_unix_hub,
  __FILE__,
  feature:    :net_unix_hub,
  depends_on: [:net_socket]
)
