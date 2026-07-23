# frozen_string_literal: true

# Kestówv 0.5.1 - net/icmp.rb
#
# ICMP simulation.
# Registers ICMP features.

module Kestowv
  module Net
    module Icmp
      @echo_replies = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:net_icmp)
          Boot.set_bit(:net_icmp)
        end

        def ping(host)
          @mutex.synchronize { @echo_replies[host] = true }
        end

        def to_a
          @echo_replies.keys
        end

        def stats
          {
            feature: :net_icmp,
            pings: @echo_replies.size
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
  :net_icmp,
  __FILE__,
  feature:    :net_icmp,
  depends_on: []
)
