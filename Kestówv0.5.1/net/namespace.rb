# frozen_string_literal: true

# Kestówv 0.5.1 - net/namespace.rb
#
# Network namespace simulation.
# Registers namespace features.

module Kestowv
  module Net
    module Namespace
      @namespaces = {}
      @mutex      = Mutex.new

      class << self
        def register_features
          Boot.register(:net_namespace)
          Boot.set_bit(:net_namespace)
        end

        def create(name)
          @mutex.synchronize { @namespaces[name] = { interfaces: [] } }
        end

        def to_a
          @namespaces.keys
        end

        def stats
          {
            feature: :net_namespace,
            count:   @namespaces.size
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
  :net_namespace,
  __FILE__,
  feature:    :net_namespace,
  depends_on: []
)
