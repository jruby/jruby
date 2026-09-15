# frozen_string_literal: true

# Kestówv 0.5.1 - net/interface.rb
#
# Network interface management.
# Registers interface features.

module Kestowv
  module Net
    module Interface
      @interfaces = {}
      @mutex      = Mutex.new

      class << self
        def register_features
          Boot.register(:net_interface)
          Boot.set_bit(:net_interface)
        end

        def create(name, device)
          @mutex.synchronize do
            @interfaces[name] = { device: device, state: :down, addresses: [] }
          end
        end

        def up(name)
          @mutex.synchronize { @interfaces[name][:state] = :up   if @interfaces[name] }
        end

        def down(name)
          @mutex.synchronize { @interfaces[name][:state] = :down if @interfaces[name] }
        end

        def add_address(name, address)
          @mutex.synchronize { @interfaces[name][:addresses] << address if @interfaces[name] }
        end

        def to_a
          @interfaces
        end

        def stats
          {
            feature: :net_interface,
            count:   @interfaces.size
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
  :net_interface,
  __FILE__,
  feature:    :net_interface,
  depends_on: []
)
