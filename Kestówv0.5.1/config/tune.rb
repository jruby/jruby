# frozen_string_literal: true

# Kestówv 0.5.1 — config/tune.rb
#
# Runtime tuning profiles.
# Applies profile values into Params and Boot.config_set.
# Profile transitions are tracked in the bit vector.

module Kestowv
  module Config
    module Tune

      PROFILES = {
        default: {
          vm:     { gc: :balanced },
          net:    { buffers: :normal },
          kernel: { scheduler: :fair }
        },
        performance: {
          vm:     { gc: :aggressive },
          net:    { buffers: :large },
          kernel: { scheduler: :realtime }
        },
        powersave: {
          vm:     { gc: :conservative },
          net:    { buffers: :small },
          kernel: { scheduler: :powersave }
        }
      }.freeze

      @current = :default
      @mutex   = Mutex.new

      class << self

        def apply(profile)
          key = profile.to_sym
          return false unless PROFILES.key?(key)

          @mutex.synchronize do
            old      = @current
            @current = key

            Boot.clear_bit(profile_bit(old)) if old != key
            Boot.set_bit(profile_bit(key))

            push_to_config(PROFILES[key])
          end

          true
        end

        def current
          @mutex.synchronize { @current }
        end

        # Returns the live settings hash for the active profile.
        # Named `active_settings` to avoid shadowing Kernel#settings
        # and to be unambiguous at call sites.
        def active_settings
          @mutex.synchronize { PROFILES[@current] }
        end

        def available
          PROFILES.keys
        end

        def to_h
          @mutex.synchronize do
            {
              current:   @current,
              available: PROFILES.keys,
              settings:  PROFILES[@current]
            }
          end
        end

        private

        def profile_bit(key)
          :"tune_profile_#{key}"
        end

        # Flatten nested profile hash into Params + extended config.
        # e.g. { vm: { gc: :aggressive } } → vm_gc = :aggressive
        def push_to_config(profile_settings)
          profile_settings.each do |category, values|
            values.each do |k, v|
              full_key = :"#{category}_#{k}"
              Params.set(full_key, v)
              Boot.config_set(full_key, v)
            end
          end
        end
      end
    end
  end
end
