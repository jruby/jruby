# frozen_string_literal: true

# Kestówv 0.5.1 — hal/cpu.rb
#
# CPU abstraction and capability tracking.
# Detects JVM/host CPU topology at boot and registers each core
# as a trackable HAL resource with Boot bit vector entries.

module Kestowv
  module Hal
    module Cpu

      @cpus  = {}
      @mutex = Mutex.new

      # Known CPU feature flags — extend as HAL grows.
      KNOWN_FEATURES = %i[
        sse sse2 sse4 avx avx2 aes
        neon vmx svm
      ].freeze

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:hal_cpu)
          Boot.set_bit(:hal_cpu)
          auto_detect
          self
        end

        # Auto-detect host CPU topology via JVM Runtime.
        # Populates logical CPUs 0..N-1 with available core count.
        def auto_detect
          count = java.lang.Runtime.getRuntime.availableProcessors rescue 1

          count.times do |i|
            add_cpu(i,
              vendor:   detect_vendor,
              model:    detect_model,
              cores:    1,
              features: detect_features
            )
          end

          self
        end

        # --------------------------------------------------------
        # CPU REGISTRY
        # --------------------------------------------------------

        def add_cpu(id, info = {})
          @mutex.synchronize do
            @cpus[id] = {
              id:       id,
              vendor:   info[:vendor]   || :unknown,
              model:    info[:model]    || :unknown,
              cores:    info[:cores]    || 1,
              features: Array(info[:features]) & KNOWN_FEATURES,
              online:   true
            }.freeze
          end

          Boot.register(cpu_bit(id))
          Boot.set_bit(cpu_bit(id))
          self
        end

        def set_online(id, online)
          @mutex.synchronize do
            entry = @cpus[id]
            return false unless entry

            # Structs are frozen — replace with updated copy
            @cpus[id] = entry.merge(online: online).freeze
          end

          online ? Boot.set_bit(cpu_bit(id)) : Boot.clear_bit(cpu_bit(id))
          true
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def get(id)
          @mutex.synchronize { @cpus[id] }
        end

        def online_ids
          @mutex.synchronize { online_ids_unsafe }
        end

        def count
          @mutex.synchronize { @cpus.size }
        end

        def online_count
          @mutex.synchronize { online_ids_unsafe.size }
        end

        def feature?(id, feature)
          entry = @mutex.synchronize { @cpus[id] }
          entry&.[](:features)&.include?(feature.to_sym) || false
        end

        def all_features
          @mutex.synchronize do
            @cpus.values.flat_map { |c| c[:features] }.uniq.sort
          end
        end

        def to_a
          @mutex.synchronize { @cpus.values.dup }
        end

        def to_h
          @mutex.synchronize { @cpus.dup }
        end

        def stats
          # Hold the lock once — avoids re-entrant deadlock from
          # calling online_ids (which also acquires @mutex).
          @mutex.synchronize do
            {
              feature:      :hal_cpu,
              total:        @cpus.size,
              online:       online_ids_unsafe.size,
              offline:      @cpus.size - online_ids_unsafe.size,
              all_features: @cpus.values.flat_map { |c| c[:features] }.uniq.sort
            }
          end
        end

        private

        # Must be called with @mutex already held.
        def online_ids_unsafe
          @cpus.select { |_, c| c[:online] }.keys
        end

        def cpu_bit(id)
          :"hal_cpu_#{id}"
        end

        # JVM-based host detection helpers.
        # Fall back gracefully if JVM introspection isn't available.

        def detect_vendor
          arch = java.lang.System.getProperty("os.arch") rescue nil
          case arch
          when /amd64|x86_64/ then :intel_amd
          when /aarch64/      then :arm
          when /ppc/          then :powerpc
          else                     :unknown
          end
        end

        def detect_model
          java.lang.System.getProperty("os.name") rescue :unknown
        end

        def detect_features
          # JVM doesn't expose CPUID directly — placeholder for
          # native extension or /proc/cpuinfo parsing via UDS.
          []
        end
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :hal_cpu,
  __FILE__,
  feature:    :hal_cpu,
  depends_on: [:core_kobject]
)
