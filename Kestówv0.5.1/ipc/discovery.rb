# frozen_string_literal: true

# Kestówv 0.5.1 - ipc/discovery.rb
#
# Service discovery simulation.
# Registers discovery features.

module Kestowv
  module Ipc
    module Discovery
      @services = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:ipc_discovery)
          Boot.set_bit(:ipc_discovery)
        end

        def register(name, endpoint)
          @mutex.synchronize { @services[name] = endpoint }
        end

        def lookup(name)
          @services[name]
        end

        def to_a
          @services.keys
        end

        def stats
          {
            feature: :ipc_discovery,
            services: @services.size
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
  :ipc_discovery,
  __FILE__,
  feature:    :ipc_discovery,
  depends_on: [:ipc]
)
