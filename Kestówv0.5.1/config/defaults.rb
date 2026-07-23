# frozen_string_literal: true

# Kestówv 0.5.1 — config/defaults.rb
#
# Default configuration values.
# Integrates with Boot.config (Struct-based) and the bit vector registry.
# Bit vector features registered here enable O(1) fast checks at runtime.

module Kestowv
  module Config
    module Defaults

      DEFAULTS = {
        scheduler:       :fair,
        gc_mode:         :balanced,
        network_buffers: :normal,
        log_level:       :info,
        hot_reload:      false,
        max_threads:     8,
        auto_version:    true,
        quiet:           false
      }.freeze

      # Keys that map directly to Boot::Config struct fields.
      # Only these are forwarded — unknown keys go to the extended registry.
      BOOT_CONFIG_KEYS = %i[auto_version quiet].freeze

      class << self

        # Apply all defaults to Boot.config and the bit vector registry.
        # Safe to call multiple times — bit vector set is idempotent.
        def apply
          DEFAULTS.each do |key, value|
            feature = feature_name(key)

            Boot.register(feature)
            Boot.set_bit(feature)

            if BOOT_CONFIG_KEYS.include?(key)
              # Forward to typed Boot::Config struct fields
              Boot.config.public_send(:"#{key}=", value)
            else
              # Extended config — store in supplemental registry
              Boot.config_set(key, value)
            end
          end

          self
        end

        # Retrieve a value — live Boot.config first, then DEFAULTS fallback.
        def get(key)
          key = key.to_sym

          if BOOT_CONFIG_KEYS.include?(key)
            Boot.config.public_send(key)
          else
            Boot.config_get(key) || DEFAULTS[key]
          end
        end

        # Set a single value — routes to the correct store.
        def set(key, value)
          key = key.to_sym

          if BOOT_CONFIG_KEYS.include?(key)
            Boot.config.public_send(:"#{key}=", value)
          else
            Boot.config_set(key, value)
          end
        end

        # Full snapshot — live values + bit vector state.
        def to_a
          DEFAULTS.map do |key, default_value|
            feature = feature_name(key)
            {
              key:     key,
              value:   get(key) || default_value,
              feature: feature,
              enabled: Boot.bit_set?(feature)
            }
          end
        end

        def to_h
          to_a.each_with_object({}) { |row, h| h[row[:key]] = row[:value] }
        end

        # Reset all keys to DEFAULTS without clearing bit vector state.
        # Use reset_hard! if you also want to clear bits.
        def reset!
          DEFAULTS.each { |key, value| set(key, value) }
          self
        end

        def reset_hard!
          DEFAULTS.each do |key, value|
            set(key, value)
            Boot.clear_bit(feature_name(key))
          end
          self
        end

        private

        def feature_name(key)
          :"default_#{key}"
        end
      end
    end
  end
end
