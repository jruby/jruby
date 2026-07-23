# frozen_string_literal: true

# Kestówv 0.5.1 — proc/env.rb
#
# Per-task environment variables.
# EnvMap is a thread-safe, copy-on-fork environment store.
# Keys are normalized to strings. Values must be strings.
# Inherits from parent env at fork; mutations don't affect parent.

module Kestowv
  module Proc
    module Env

      # Keys that are never inherited across exec or sanitized envs.
      SENSITIVE_KEYS = %w[
        LD_PRELOAD LD_LIBRARY_PATH
        RUBYOPT RUBYLIB
        DYLD_INSERT_LIBRARIES DYLD_LIBRARY_PATH
      ].freeze

      # Minimal safe environment for sanitized exec contexts.
      SAFE_DEFAULTS = {
        "PATH"    => "/usr/local/bin:/usr/bin:/bin",
        "HOME"    => "/",
        "LANG"    => "en_US.UTF-8",
        "TERM"    => "dumb"
      }.freeze

      # --------------------------------------------------------
      # ENVMAP — per-task environment store
      # --------------------------------------------------------

      class EnvMap
        def initialize(initial = {})
          @env   = normalize(initial)
          @mutex = Mutex.new
        end

        # --------------------------------------------------------
        # READ
        # --------------------------------------------------------

        def get(key)
          @mutex.synchronize { @env[key.to_s] }
        end
        alias_method :[], :get

        def key?(key)
          @mutex.synchronize { @env.key?(key.to_s) }
        end

        def keys
          @mutex.synchronize { @env.keys.dup }
        end

        def to_h
          @mutex.synchronize { @env.dup }
        end

        def size
          @mutex.synchronize { @env.size }
        end

        # --------------------------------------------------------
        # WRITE
        # --------------------------------------------------------

        def set(key, value)
          raise ArgumentError, "Key must be String"   unless key.respond_to?(:to_s)
          raise ArgumentError, "Value must be String" unless value.respond_to?(:to_s)

          k = key.to_s
          raise ArgumentError, "Key cannot contain '='" if k.include?("=")
          raise ArgumentError, "Key cannot be empty"    if k.empty?

          @mutex.synchronize { @env[k] = value.to_s }
          self
        end
        alias_method :[]=, :set

        def delete(key)
          @mutex.synchronize { @env.delete(key.to_s) }
        end

        def merge!(other)
          normalized = normalize(other.respond_to?(:to_h) ? other.to_h : other)
          @mutex.synchronize { @env.merge!(normalized) }
          self
        end

        # --------------------------------------------------------
        # FORK / EXEC SEMANTICS
        # --------------------------------------------------------

        # Copy-on-fork — produce an independent child env.
        def fork
          @mutex.synchronize { EnvMap.new(@env.dup) }
        end

        # Sanitized copy for exec — strips sensitive keys.
        def sanitize
          @mutex.synchronize do
            clean = @env.reject { |k, _| SENSITIVE_KEYS.include?(k) }
            EnvMap.new(clean)
          end
        end

        # Minimal safe env — only SAFE_DEFAULTS, nothing else.
        def self.safe_env
          new(SAFE_DEFAULTS.dup)
        end

        # Build from current Ruby process ENV.
        def self.from_host
          new(ENV.to_h)
        end

        # --------------------------------------------------------
        # INTROSPECTION
        # --------------------------------------------------------

        def to_a
          @mutex.synchronize do
            @env.map { |k, v| "#{k}=#{v}" }.sort
          end
        end

        def inspect
          "#<EnvMap size=#{size}>"
        end

        private

        def normalize(hash)
          hash.each_with_object({}) do |(k, v), h|
            key = k.to_s
            next if key.empty? || key.include?("=")
            h[key] = v.to_s
          end
        end
      end

      # --------------------------------------------------------
      # MODULE INTERFACE
      # --------------------------------------------------------

      @envs_issued = 0
      @mutex       = Mutex.new

      class << self

        def register_with_boot
          Boot.register(:proc_env)
          Boot.set_bit(:proc_env)
          self
        end

        # Create a new EnvMap with optional initial values.
        def create(initial = {})
          env = EnvMap.new(initial)
          @mutex.synchronize { @envs_issued += 1 }
          env
        end

        # Create from host Ruby ENV (for boot-time tasks).
        def from_host
          env = EnvMap.from_host.sanitize
          @mutex.synchronize { @envs_issued += 1 }
          env
        end

        # Minimal safe environment — for sandboxed exec.
        def safe
          env = EnvMap.safe_env
          @mutex.synchronize { @envs_issued += 1 }
          env
        end

        def stats
          @mutex.synchronize do
            {
              feature:      :proc_env,
              envs_issued:  @envs_issued,
              sensitive_keys: SENSITIVE_KEYS.size
            }
          end
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_env,
  __FILE__,
  feature:    :proc_env,
  depends_on: [:proc_task]
)
