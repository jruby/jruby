# frozen_string_literal: true

# Kestówv 0.5.1 - net/neighbor.rb
#
# Neighbor / ARP table simulation.
# Registers neighbor features.

module Kestowv
  module Net
    module Neighbor
      @neighbors = {}
      @mutex     = Mutex.new

      class << self
        def register_features
          Boot.register(:net_neighbor)
          Boot.set_bit(:net_neighbor)
        end

        def add(ip, mac, interface)
          @mutex.synchronize do
            @neighbors[ip] = { mac: mac, interface: interface }
          end
        end

        def to_a
          @neighbors
        end

        def stats
          {
            feature: :net_neighbor,
            count:   @neighbors.size
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
  :net_neighbor,
  __FILE__,
  feature:    :net_neighbor,
  depends_on: []
)
