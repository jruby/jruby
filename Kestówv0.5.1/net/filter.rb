# frozen_string_literal: true

# Kestówv 0.5.1 - net/filter.rb
#
# Packet filter simulation.
# Registers filter features.

module Kestowv
  module Net
    module Filter
      @rules = []
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:net_filter)
          Boot.set_bit(:net_filter)
        end

        def add_rule(action, condition)
          @mutex.synchronize do
            @rules << { action: action, condition: condition }
          end
        end

        def to_a
          @rules
        end

        def stats
          {
            feature: :net_filter,
            rules: @rules.size
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
  :net_filter,
  __FILE__,
  feature:    :net_filter,
  depends_on: []
)
