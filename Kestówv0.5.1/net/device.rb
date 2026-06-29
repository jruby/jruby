# frozen_string_literal: true

# Kestówv 0.5.1 - net/device.rb
#
# Network device abstraction.
# Registers network device features.

module Kestowv
  module Net
    module Device
      @devices = {}
      @mutex   = Mutex.new

      class << self
        def register_features
          Boot.register(:net_device)
          Boot.set_bit(:net_device)
        end

        def register(name, type)
          @mutex.synchronize do
            @devices[name] = { type: type, state: :down }
          end
        end

        def up(name)
          @mutex.synchronize { @devices[name][:state] = :up if @devices[name] }
        end

        def down(name)
          @mutex.synchronize { @devices[name][:state] = :down if @devices[name] }
        end

        def list
          @devices.keys
        end

        def to_a
          @devices
        end

        def stats
          {
            feature: :net_device,
            count:   @devices.size
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
  :net_device,
  __FILE__,
  feature:    :net_device,
  depends_on: []
)
