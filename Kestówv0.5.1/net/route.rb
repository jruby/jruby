# frozen_string_literal: true

# Kestówv 0.5.1 - net/route.rb
#
# Routing table simulation.
# Registers route features.

module Kestowv
  module Net
    module Route
      @routes = []
      @mutex  = Mutex.new

      class << self
        def register_features
          Boot.register(:net_route)
          Boot.set_bit(:net_route)
        end

        def add(destination, gateway, interface)
          @mutex.synchronize do
            @routes << { destination: destination, gateway: gateway, interface: interface }
          end
        end

        def to_a
          @routes
        end

        def stats
          {
            feature: :net_route,
            routes:  @routes.size
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
  :net_route,
  __FILE__,
  feature:    :net_route,
  depends_on: []
)
