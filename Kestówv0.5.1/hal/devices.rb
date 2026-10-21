# frozen_string_literal: true

# Kestówv 0.5.1 — hal/devices.rb
#
# Device registry and abstraction.
# Tracks physical and virtual devices with type classification,
# online/offline state, and Boot bit vector entries per device.

module Kestowv
  module Hal
    module Devices

      @devices = {}
      @mutex   = Mutex.new

      DEVICE_TYPES = %i[
        block
        char
        network
        timer
        console
        virtual
        generic
      ].freeze

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:hal_devices)
          Boot.set_bit(:hal_devices)
          self
        end

        # --------------------------------------------------------
        # DEVICE REGISTRY
        # --------------------------------------------------------

        def register_device(name, type: :generic, info: {}, irq: nil)
          raise ArgumentError, "Unknown device type: #{type}" unless DEVICE_TYPES.include?(type)

          key = name.to_sym

          @mutex.synchronize do
            next if @devices.key?(key)  # idempotent

            @devices[key] = {
              name:       key,
              type:       type,
              info:       info.freeze,
              irq:        irq,
              online:     true,
              registered_at: Time.now.freeze
            }
          end

          Boot.register(device_bit(key))
          Boot.set_bit(device_bit(key))
          self
        end

        def deregister_device(name)
          key = name.to_sym
          @mutex.synchronize { @devices.delete(key) }
          Boot.clear_bit(device_bit(key))
          true
        end

        def set_online(name, state)
          key = name.to_sym
          @mutex.synchronize do
            entry = @devices[key]
            return false unless entry
            entry[:online] = state
          end

          state ? Boot.set_bit(device_bit(key)) : Boot.clear_bit(device_bit(key))
          true
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def get(name)
          @mutex.synchronize { @devices[name.to_sym] }
        end

        def online?(name)
          @mutex.synchronize { @devices[name.to_sym]&.[](:online) || false }
        end

        def by_type(type)
          @mutex.synchronize do
            @devices.values.select { |d| d[:type] == type }.dup
          end
        end

        def by_irq(irq)
          @mutex.synchronize do
            @devices.values.select { |d| d[:irq] == irq }.dup
          end
        end

        def to_a
          @mutex.synchronize do
            @devices.values.map(&:dup).sort_by { |d| d[:name].to_s }
          end
        end

        def stats
          @mutex.synchronize do
            by_type = DEVICE_TYPES.each_with_object({}) do |type, h|
              h[type] = @devices.count { |_, d| d[:type] == type }
            end

            {
              feature:  :hal_devices,
              total:    @devices.size,
              online:   @devices.count { |_, d| d[:online] },
              offline:  @devices.count { |_, d| !d[:online] },
              by_type:  by_type
            }
          end
        end

        private

        def device_bit(key)
          :"hal_device_#{key}"
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :hal_devices,
  __FILE__,
  feature:    :hal_devices,
  depends_on: [:hal_cpu, :hal_interrupts]
)
