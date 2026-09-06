# frozen_string_literal: true

# Kestówv 0.5.1 — config/loader.rb
#
# Central configuration loader.
# Loads defaults + params in the correct phase order,
# routes all file I/O through Boot.byte_dispatch,
# and gates output on Boot.config.quiet.

module Kestowv
  module Config
    module Loader

      # Supported param file extensions — drives both glob and fallback candidates.
      PARAM_EXTENSIONS = %w[.conf .cfg .json .yaml .yml].freeze

      # Top-level candidate filenames, checked in priority order.
      TOP_LEVEL_CANDIDATES = %w[
        kestowv.conf
        params.conf
        settings.json
        config.json
      ].freeze

      class << self

        # Load all configuration in phase order:
        #   1. Defaults  — typed Boot.config fields + bit vector registration
        #   2. Params dir — per-subsystem override files
        #   3. Top-level  — root config files, checked in priority order
        #
        # Returns self for chaining: Loader.load_all!.validate!
        def load_all!(config_dir: "config")
          log "Loading configuration from #{config_dir}/"

          load_defaults
          load_params_directory(File.join(config_dir, "params"))
          load_top_level_configs(config_dir)

          log "Configuration loaded — #{Params.stats[:enabled]}/#{Params.stats[:total]} params active"
          self
        end

        # Optional: raise on missing required params after load.
        def validate!(required_keys = [])
          missing = required_keys.map(&:to_sym).reject { |k| Params.get(k) }
          raise "[Config] Missing required params: #{missing.join(', ')}" unless missing.empty?
          self
        end

        private

        # ----------------------------------------------------------
        # PHASE 1 — Defaults
        # ----------------------------------------------------------

        def load_defaults
          Defaults.apply
          log "Defaults applied"
        end

        # ----------------------------------------------------------
        # PHASE 2 — Params directory
        # ----------------------------------------------------------

        def load_params_directory(dir)
          return unless Dir.exist?(dir)

          glob = File.join(dir, "*.{#{PARAM_EXTENSIONS.map { |e| e.delete('.') }.join(',')}}}")

          files = Dir.glob(glob).sort  # sort for deterministic load order
          files.each do |path|
            load_param_file(path)
          end
        end

        # ----------------------------------------------------------
        # PHASE 3 — Top-level candidates
        # ----------------------------------------------------------

        def load_top_level_configs(dir)
          TOP_LEVEL_CANDIDATES.each do |name|
            path = File.join(dir, name)
            load_param_file(path) if File.exist?(path)
          end
        end

        # ----------------------------------------------------------
        # SHARED FILE LOADER
        # Routes through Boot.byte_dispatch before touching content.
        # ----------------------------------------------------------

        def load_param_file(path)
          bc = Boot.byte_dispatch(path)

          if bc.skip?
            log "Skipped #{File.basename(path)} (#{bc})", level: :debug
            return false
          end

          result = Params.load_from_file(path)
          log "#{result ? '✓' : '✗'} #{File.basename(path)} (#{bc.kind})"
          result
        end

        # ----------------------------------------------------------
        # LOGGING — gated on Boot.config.quiet
        # ----------------------------------------------------------

        def log(msg, level: :info)
          return if Boot.config.quiet && level == :debug
          return if Boot.config.quiet && level == :info

          prefix = level == :debug ? "[Config:debug]" : "[Config]"
          puts "  #{prefix} #{msg}"
        end
      end
    end
  end
end
