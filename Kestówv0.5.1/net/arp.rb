# frozen_string_literal: true

# Kestówv 0.5.1 - net/arp.rb
#
# ARP simulation.
# Registers ARP features.

module Kestowv
  module Net
    module Arp
      @table = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:net_arp)
          Boot.set_bit(:net_arp)
        end

        def add(ip, mac)
          @mutex.synchronize { @table[ip] = mac }
        end

        def resolve(ip)
          @table[ip]
        end

        def to_a
          @table
        end

        def stats
          {
            feature: :net_arp,
            entries: @table.size
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
  :net_arp,
  __FILE__,
  feature:    :net_arp,
  depends_on: []
)
