# frozen_string_literal: true

# Kestówv 0.5.1 — ipc/namespace.rb
#
# Namespace-scoped IPC enforcement.
# Channels are bound to a NamespaceSet (from Task.ns_set).
# Isolated tasks cannot reach channels outside their namespace.
#
# Wire format: MessagePack (binary, default).
# JSON only in debug mode (Boot.config.ipc_debug = true).
# Never use JSON in production — it leaks structure and is 3-5x larger.

module Kestowv
  module Ipc
    module Namespace

      # --------------------------------------------------------
      # SERIALIZATION — MessagePack default, JSON debug-only
      # --------------------------------------------------------

      module Serializer
        # Attempt MessagePack first — required for production framing.
        begin
          require "msgpack"
          BACKEND = :msgpack
        rescue LoadError
          # msgpack gem not available — fall back with a loud warning.
          # This should never happen in a production Kestówv environment.
          warn "[IPC::Namespace] WARNING: msgpack not available — falling back to Marshal. " \
               "Run: gem install msgpack"
          BACKEND = :marshal
        end

        def self.encode(obj)
          case BACKEND
          when :msgpack then MessagePack.pack(obj)
          when :marshal then Marshal.dump(obj)
          end
        end

        def self.decode(bytes)
          case BACKEND
          when :msgpack then MessagePack.unpack(bytes)
          when :marshal then Marshal.load(bytes)
          end
        end

        # JSON is ONLY for human-readable debug output — never for wire framing.
        def self.to_debug_json(obj)
          require "json"
          JSON.pretty_generate(obj)
        rescue LoadError
          obj.inspect
        end
      end

      # --------------------------------------------------------
      # TAGGED MESSAGE — namespace-scoped envelope
      # --------------------------------------------------------

      TaggedMessage = Struct.new(:ns_id, :ipc_ns_id, :payload, :sent_at, keyword_init: true) do
        def encode
          Serializer.encode(to_h.transform_keys(&:to_s))
        end

        def self.decode(bytes)
          data = Serializer.decode(bytes)
          new(
            ns_id:     data["ns_id"],
            ipc_ns_id: data["ipc_ns_id"],
            payload:   data["payload"],
            sent_at:   data["sent_at"]
          )
        end

        def debug_inspect
          Serializer.to_debug_json(to_h)
        end
      end

      # --------------------------------------------------------
      # NAMESPACE BINDING REGISTRY
      # --------------------------------------------------------

      @bindings = {}   # channel object_id => { ns_set:, ipc_ns_id: }
      @mutex    = Mutex.new

      class << self

        def register_with_boot
          Boot.register(:ipc_namespace)
          Boot.set_bit(:ipc_namespace)
          self
        end

        # Bind a Channel to a NamespaceSet.
        # ipc_ns_id: the specific IPC namespace ID from the task's NsSet.
        def bind(channel, ns_set)
          ipc_ns_id = ns_set.ns_id(:ipc)

          @mutex.synchronize do
            @bindings[channel.object_id] = {
              ns_set:    ns_set,
              ipc_ns_id: ipc_ns_id
            }
          end

          Boot.set_bit(ns_bit(ipc_ns_id)) if ipc_ns_id
          channel
        end

        def unbind(channel)
          binding = @mutex.synchronize { @bindings.delete(channel.object_id) }
          Boot.clear_bit(ns_bit(binding[:ipc_ns_id])) if binding&.[](:ipc_ns_id)
        end

        # Check if a channel is allowed to communicate with another.
        # nil ns_set = root namespace — always allowed.
        def allowed?(channel, ns_set)
          binding = @mutex.synchronize { @bindings[channel.object_id] }
          return true if binding.nil?   # unbound = root = always allowed

          ch_ipc_ns  = binding[:ipc_ns_id]
          req_ipc_ns = ns_set.ns_id(:ipc)

          ch_ipc_ns == req_ipc_ns
        end

        # Wrap a payload in a namespace-tagged envelope for sending.
        def encode_message(payload, ns_set:)
          msg = TaggedMessage.new(
            ns_id:     ns_set&.ns_id(:pid),
            ipc_ns_id: ns_set&.ns_id(:ipc),
            payload:   payload,
            sent_at:   Time.now.to_f
          )
          msg.encode
        end

        # Decode and validate an incoming message against expected ns.
        # Returns the payload or raises if namespace mismatch.
        def decode_message(bytes, expected_ns_set: nil)
          msg = TaggedMessage.decode(bytes)

          if expected_ns_set && !namespace_match?(msg, expected_ns_set)
            raise NamespaceMismatchError,
              "IPC namespace violation: got #{msg.ipc_ns_id}, " \
              "expected #{expected_ns_set.ns_id(:ipc)}"
          end

          # Debug-only logging — never in production
          if Boot.config.respond_to?(:ipc_debug) && Boot.config.ipc_debug
            warn "[IPC::Namespace:debug] #{msg.debug_inspect}"
          end

          msg.payload
        end

        def stats
          @mutex.synchronize do
            {
              feature:  :ipc_namespace,
              bound:    @bindings.size,
              backend:  Serializer::BACKEND
            }
          end
        end

        private

        def namespace_match?(msg, ns_set)
          msg.ipc_ns_id == ns_set.ns_id(:ipc)
        end

        def ns_bit(ipc_ns_id)
          :"ipc_ns_#{ipc_ns_id}"
        end
      end

      # --------------------------------------------------------
      # ERRORS
      # --------------------------------------------------------

      class NamespaceMismatchError < RuntimeError; end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :ipc_namespace,
  __FILE__,
  feature:    :ipc_namespace,
  depends_on: [:ipc_channel, :proc_namespace]
)
