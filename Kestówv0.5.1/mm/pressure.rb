# frozen_string_literal: true

# Kestówv 0.5.1 - mm/pressure.rb
#
# Memory pressure detection.
# Registers pressure features.

module Kestowv
  module Mm
    module Pressure
      @level = :low

      class << self
        def register_features
          Boot.register(:mm_pressure)
          Boot.set_bit(:mm_pressure)
        end

        def set_level(level)
          @level = level
        end

        def current
          @level
        end

        def to_a
          { level: @level }
        end

        def stats
          {
            feature: :mm_pressure,
            level: @level
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
  :mm_pressure,
  __FILE__,
  feature:    :mm_pressure,
  depends_on: [:mm_physical]
)
