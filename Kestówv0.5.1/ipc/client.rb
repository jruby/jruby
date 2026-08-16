# frozen_string_literal: true

# Kestówv 0.5.1 — ipc/client.rb
#
# Convenience client wrapper around Channel.
# Adds retry logic, request/response pattern, and namespace tagging.

module Kestowv
  module Ipc
    class Client

      DEFAULT_RETRIES = 3
      RETRY_DELAY     = 0.1   # seconds between retries

      attr_reader :channel, :addr

      # --------------------------------------------------------
      # LIFECYCLE
      # --------------------------------------------------------

      def initialize(addr:    Channel::IPC_ADDR,
                     retries: DEFAULT_RETRIES,
                     ns_id:   nil)
        @addr    = addr
        @retries = retries
        @ns_id   = ns_id   # namespace scope — nil means root ns
        @channel = Channel.new(addr: addr)
        @mutex   = Mutex.new
      end

      # Connect with retry across all three tiers.
      def connect
        attempts = 0
        begin
          @channel.connect
          Boot.set_bit(:ipc_client_connected)
          self
        rescue Channel::ConnectionError => e
          attempts += 1
          if attempts <= @retries
            warn "[IPC::Client] Retry #{attempts}/#{@retries} — #{e.message}" unless Boot.config.quiet
            sleep RETRY_DELAY
            retry
          end
          raise
        end
      end

      def disconnect
        @channel.disconnect
        Boot.clear_bit(:ipc_client_connected)
        self
      end

      def connected?
        @channel.connected?
      end

      # --------------------------------------------------------
      # MESSAGING
      # --------------------------------------------------------

      def send_message(data)
        @channel.send_message(data)
        self
      end

      def receive_message
        @channel.receive_message
      end

      # Request/response — send then block for reply.
      def request(data)
        @mutex.synchronize do
          @channel.send_message(data)
          @channel.receive_message
        end
      end

      # --------------------------------------------------------
      # NAMESPACE AWARENESS
      # --------------------------------------------------------

      # Tag outgoing messages with the client's namespace ID.
      # Server can use this to enforce namespace isolation.
      def send_tagged(data)
        tagged = { ns_id: @ns_id, payload: data }
        send_message(tagged.inspect)   # TODO: replace with msgpack/CBOR when available
      end

      def ns_id
        @ns_id
      end

      # --------------------------------------------------------
      # BLOCK FORM — auto connect/disconnect
      # --------------------------------------------------------

      def self.open(addr: Channel::IPC_ADDR, ns_id: nil, &block)
        client = new(addr: addr, ns_id: ns_id)
        client.connect
        block.call(client)
      ensure
        client&.disconnect
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def stats
        @channel.stats
      end

      def to_h
        {
          addr:      @addr,
          ns_id:     @ns_id,
          tier:      @channel.tier,
          connected: connected?,
          stats:     stats
        }
      end

      def to_s
        "#<IPC::Client addr=#{@addr} ns=#{@ns_id} connected=#{connected?}>"
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :ipc_client,
  __FILE__,
  feature:    :ipc_client,
  depends_on: [:ipc_channel]
)
