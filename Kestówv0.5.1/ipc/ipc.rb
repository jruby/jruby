# frozen_string_literal: true

# Kestówv 0.5.1 — ipc/ipc.rb
#
# Top-level IPC orchestrator (canonical location inside ipc/).
# Loads all IPC submodules in dependency order and exposes
# a clean factory interface for the rest of the kernel.

module Kestowv
  module Ipc

    @mutex = Mutex.new
    @initialized = false

    class << self

      # --------------------------------------------------------
      # BOOT REGISTRATION
      # --------------------------------------------------------

      def register_with_boot
        Boot.register(:ipc)
        Boot.set_bit(:ipc)
        self
      end

      # --------------------------------------------------------
      # FACTORY INTERFACE
      # --------------------------------------------------------

      # Open a connected client channel.
      # Block form auto-disconnects on exit.
      def client(addr: Channel::IPC_ADDR, ns_set: nil, &block)
        c = Client.new(addr: addr, ns_id: ns_set&.ns_id(:ipc))
        c.connect

        if block
          begin
            block.call(c)
          ensure
            c.disconnect
          end
        else
          c
        end
      end

      # Start a listening server.
      # Block form runs accept_loop and closes on exit.
      def server(addr: Channel::IPC_ADDR, &block)
        s = Server.new(addr: addr)
        s.listen

        if block
          begin
            s.accept_loop(&block)
          ensure
            s.close
          end
        else
          s
        end
      end

      # Raw channel (unconnected) — caller manages lifecycle.
      def channel(addr: Channel::IPC_ADDR)
        Channel.new(addr: addr)
      end

      # Namespace-bound client — enforces IPC isolation.
      def scoped_client(ns_set:, addr: Channel::IPC_ADDR)
        c = Client.new(addr: addr, ns_id: ns_set.ns_id(:ipc))
        c.connect
        Namespace.bind(c.channel, ns_set)
        c
      end

      # --------------------------------------------------------
      # SERIALIZATION SHORTHAND
      # --------------------------------------------------------

      def encode(payload, ns_set: nil)
        Namespace.encode_message(payload, ns_set: ns_set)
      end

      def decode(bytes, expected_ns_set: nil)
        Namespace.decode_message(bytes, expected_ns_set: expected_ns_set)
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def stats
        {
          feature:   :ipc,
          backend:   Namespace::Serializer::BACKEND,
          namespace: Namespace.stats,
          channel:   { addr: Channel::IPC_ADDR, tier: Channel::TIERS }
        }
      end

      def to_s
        "#<Kestowv::Ipc backend=#{Namespace::Serializer::BACKEND}>"
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :ipc,
  __FILE__,
  feature:    :ipc,
  depends_on: [:ipc_channel, :ipc_server, :ipc_client, :ipc_namespace]
)
