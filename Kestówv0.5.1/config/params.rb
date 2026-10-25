# frozen_string_literal: true

# Kestówv 0.5.1 — config/params.rb
#
# Sysctl-style kernel parameters with Boot integration.
# Supports loading from files via Boot.load / byte_dispatch.
# Thread-safe: all param state mutations go through @mutex.

module Kestowv
  module Config
    module Params

      @params = {}
      @mutex  = Mutex.new

      # Coerce common string representations to typed Ruby values.
      # Runs on every value coming in from key=value files.
      COERCIONS = [
        [ /\Atrue\z/i,  ->(_) { true  } ],
        [ /\Afalse\z/i, ->(_) { false } ],
        [ /\A\d+\z/,    ->(v) { v.to_i } ],
        [ /\A\d+\.\d+\z/, ->(v) { v.to_f } ],
      ].freeze

      class << self

        # ----------------------------------------------------------
        # REGISTRATION
        # ----------------------------------------------------------

        def register(name, default: nil, description: nil)
          key = name.to_sym
          @mutex.synchronize do
            # Don't overwrite an existing live registration
            next if @params.key?(key)

            @params[key] = {
              value:       default,
              default:     default,
              description: description
            }
            Boot.register(key)
          end
          self
        end

        # ----------------------------------------------------------
        # READ / WRITE
        # ----------------------------------------------------------

        def set(name, value)
          key = name.to_sym
          @mutex.synchronize do
            return false unless @params.key?(key)

            @params[key][:value] = value

            # Bit vector: set on truthy, clear on falsy
            value ? Boot.set_bit(key) : Boot.clear_bit(key)
            true
          end
        end

        def get(name)
          @mutex.synchronize { @params.dig(name.to_sym, :value) }
        end

        def enabled?(name)
          Boot.bit_set?(name.to_sym)
        end

        def reset(name)
          key = name.to_sym
          @mutex.synchronize do
            return false unless @params.key?(key)

            @params[key][:value] = @params[key][:default]
            Boot.clear_bit(key)
            true
          end
        end

        def reset_all!
          @mutex.synchronize do
            @params.each do |key, info|
              info[:value] = info[:default]
              Boot.clear_bit(key)
            end
          end
          self
        end

        # ----------------------------------------------------------
        # FILE LOADING
        # Uses Boot.byte_dispatch to route before reading content.
        # ----------------------------------------------------------

        def load_from_file(path)
          return false unless File.exist?(path)

          bc = Boot.byte_dispatch(path)
          return false if bc.skip?

          case bc.kind
          when :config_json  then load_json(path)
          when :config_yaml  then load_yaml(path)
          when :config_file  then load_keyvalue(path)
          else
            warn "[Params] Unrecognised config format: #{path} (#{bc})"
            false
          end
        end

        # ----------------------------------------------------------
        # INTROSPECTION
        # ----------------------------------------------------------

        def all
          @mutex.synchronize { @params.keys.dup }
        end

        def to_a
          @mutex.synchronize do
            @params.map do |name, info|
              {
                name:        name,
                value:       info[:value],
                default:     info[:default],
                description: info[:description],
                enabled:     Boot.bit_set?(name)
              }
            end
          end
        end

        def to_h
          @mutex.synchronize do
            @params.transform_values { |info| info[:value] }
          end
        end

        def stats
          @mutex.synchronize do
            {
              total:   @params.size,
              enabled: @params.count { |name, _| Boot.bit_set?(name) }
            }
          end
        end

        private

        # ----------------------------------------------------------
        # FORMAT LOADERS
        # ----------------------------------------------------------

        def load_json(path)
          require "json"
          data = JSON.parse(File.read(path))
          data.each { |k, v| register_and_set(k, v) }
          true
        rescue JSON::ParserError => e
          warn "[Params] JSON parse error in #{path}: #{e.message}"
          false
        end

        def load_yaml(path)
          require "yaml"
          data = YAML.safe_load(File.read(path), symbolize_names: false) || {}
          data.each { |k, v| register_and_set(k, v) }
          true
        rescue => e
          warn "[Params] YAML parse error in #{path}: #{e.message}"
          false
        end

        def load_keyvalue(path)
          File.open(path).each_line do |line|
            line = line.strip
            next if line.empty? || line.start_with?("#")

            if line =~ /\A([^=]+)=(.*)\z/
              register_and_set($1.strip, coerce($2.strip))
            else
              warn "[Params] Skipping malformed line: #{line.inspect}"
            end
          end
          true
        rescue => e
          warn "[Params] key=value parse error in #{path}: #{e.message}"
          false
        end

        def register_and_set(name, value)
          key = name.to_sym
          register(key, default: value) unless @params.key?(key)
          set(key, value)
        end

        def coerce(str)
          match = COERCIONS.find { |pattern, _| str.match?(pattern) }
          match ? match[1].call(str) : str
        end
      end
    end
  end
end
