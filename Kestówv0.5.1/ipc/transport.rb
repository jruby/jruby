# frozen_string_literal: true

# Kestówv 0.5.1 - ipc/transport.rb
#
# IPC transport abstraction.
# Registers transport features.

module Kestowv
  module Ipc
    module Transport
      @transports = {}
      @mutex      = Mutex.new

      class << self
        def register_features
          Boot.register(:ipc_transport)
          Boot.set_bit(:ipc_transport)
        end

        def register(name, impl)
          @mutex.synchronize { @transports[name] = impl }
        end

        def get(name)
          @transports[name]
        end

        def to_a
          @transports.keys
        end

        def stats
          {
            feature:    :ipc_transport,
            transports: @transports.size
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
  :ipc_transport,
  __FILE__,
  feature:    :ipc_transport,
  depends_on: [:ipc]
)
