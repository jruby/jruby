# frozen_string_literal: true

# Kestówv 0.5.1 — proc/limits.rb
#
# Resource limits (rlimit-style).
# Each limit has a soft (current) and hard (ceiling) value.
# Soft limits can be raised up to the hard limit by the process itself.
# Hard limits can only be raised by root (CAP_SYS_RESOURCE).

module Kestowv
  module Proc
    module Limits

      # --------------------------------------------------------
      # LIMIT NAMES — mirrors Linux rlimit constants
      # --------------------------------------------------------

      RESOURCES = %i[
        cpu       # max CPU time in seconds
        fsize     # max file size in bytes
        data      # max data segment size in bytes
        stack     # max stack size in bytes
        core      # max core dump size in bytes
        nofile    # max open file descriptors
        nproc     # max number of processes
        as        # max virtual memory (address space) in bytes
        memlock   # max locked memory in bytes
      ].freeze

      RLIM_INFINITY = Float::INFINITY

      # Default soft/hard pairs — [soft, hard]
      DEFAULTS = {
        cpu:     [60,       120],
        fsize:   [1 << 30,  1 << 31],
        data:    [1 << 30,  1 << 31],
        stack:   [8 << 20,  16 << 20],
        core:    [0,        1 << 30],
        nofile:  [1024,     4096],
        nproc:   [1024,     4096],
        as:      [1 << 32,  RLIM_INFINITY],
        memlock: [64 << 10, 64 << 10]
      }.freeze

      # --------------------------------------------------------
      # RLIMIT — immutable limit pair
      # --------------------------------------------------------

      RLimit = Struct.new(:resource, :soft, :hard, keyword_init: true) do
        def exceeded?(value)
          soft != RLIM_INFINITY && value > soft
        end

        def hard_exceeded?(value)
          hard != RLIM_INFINITY && value > hard
        end

        # Raise soft limit — capped at hard.
        def raise_soft(new_soft)
          raise ArgumentError, "soft cannot exceed hard" if new_soft > hard
          RLimit.new(resource: resource, soft: new_soft, hard: hard)
        end

        # Raise hard limit — requires elevated privilege (enforced by caller).
        def raise_hard(new_hard)
          RLimit.new(resource: resource, soft: soft, hard: new_hard)
        end

        def to_s
          soft_s = soft  == RLIM_INFINITY ? "unlimited" : soft.to_s
          hard_s = hard  == RLIM_INFINITY ? "unlimited" : hard.to_s
          "#{resource}=[#{soft_s}/#{hard_s}]"
        end
      end

      # --------------------------------------------------------
      # LIMIT SET — per-task collection of RLimits
      # --------------------------------------------------------

      class LimitSet
        def initialize(limits = {})
          @limits = DEFAULTS.each_with_object({}) do |(res, pair), h|
            override = limits[res]
            soft, hard = override || pair
            h[res] = RLimit.new(resource: res, soft: soft, hard: hard)
          end
          @mutex = Mutex.new
        end

        def get(resource)
          @mutex.synchronize { @limits[resource.to_sym] }
        end

        # Set soft limit — raises if above hard.
        def set_soft(resource, value)
          @mutex.synchronize do
            rl = @limits[resource.to_sym]
            return false unless rl
            @limits[resource.to_sym] = rl.raise_soft(value)
            true
          end
        end

        # Set hard limit — caller must verify CAP_SYS_RESOURCE.
        def set_hard(resource, value)
          @mutex.synchronize do
            rl = @limits[resource.to_sym]
            return false unless rl
            @limits[resource.to_sym] = rl.raise_hard(value)
            true
          end
        end

        def exceeded?(resource, value)
          get(resource)&.exceeded?(value) || false
        end

        def to_a
          @mutex.synchronize { @limits.values.map(&:to_s) }
        end

        def to_h
          @mutex.synchronize do
            @limits.transform_values { |rl| { soft: rl.soft, hard: rl.hard } }
          end
        end
      end

      # --------------------------------------------------------
      # MODULE INTERFACE
      # --------------------------------------------------------

      @mutex = Mutex.new
      @sets_issued = 0

      class << self

        def register_with_boot
          Boot.register(:proc_limits)
          Boot.set_bit(:proc_limits)
          self
        end

        # Create a new LimitSet with optional per-resource overrides.
        # overrides: { nofile: [2048, 8192], nproc: [512, 2048] }
        def create(overrides = {})
          validated = overrides.select { |k, _| RESOURCES.include?(k.to_sym) }
          set = LimitSet.new(validated)
          @mutex.synchronize { @sets_issued += 1 }
          set
        end

        # Default limit set — no overrides.
        def default
          create
        end

        # Check a resource value against a LimitSet.
        # Returns :ok, :soft_exceeded, or :hard_exceeded.
        def check(limit_set, resource, value)
          rl = limit_set.get(resource)
          return :unknown unless rl

          if rl.hard_exceeded?(value)
            :hard_exceeded
          elsif rl.exceeded?(value)
            :soft_exceeded
          else
            :ok
          end
        end

        def stats
          @mutex.synchronize do
            {
              feature:      :proc_limits,
              sets_issued:  @sets_issued,
              resources:    RESOURCES.size,
              defaults:     DEFAULTS.transform_values { |s, h| { soft: s, hard: h } }
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
  :proc_limits,
  __FILE__,
  feature:    :proc_limits,
  depends_on: [:proc_task, :proc_credentials]
)
