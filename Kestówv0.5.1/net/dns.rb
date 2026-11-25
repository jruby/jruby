# frozen_string_literal: true

# Kestówv 0.5.1 - net/dns.rb
#
# DNS simulation.
# Registers DNS features.

module Kestowv
  module Net
    module Dns
      @records = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:net_dns)
          Boot.set_bit(:net_dns)
        end

        def add_record(name, ip)
          @mutex.synchronize { @records[name] = ip }
        end

        def resolve(name)
          @records[name]
        end

        def to_a
          @records
        end

        def stats
          {
            feature: :net_dns,
            records: @records.size
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
  :net_dns,
  __FILE__,
  feature:    :net_dns,
  depends_on: []
)
