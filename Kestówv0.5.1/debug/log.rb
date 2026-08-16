# frozen_string_literal: true

# Kestówv 0.5.1 - debug/log.rb
#
# Debug logging layer.
# Uses Klog internally and registers debug features.

module Kestowv
  module Debug
    module Log
      class << self
        def register_features
          Boot.register(:debug_log)
          Boot.set_bit(:debug_log)
        end

        def info(msg)  = Kestowv::Core::Klog.info("[DEBUG] #{msg}")
        def warn(msg)  = Kestowv::Core::Klog.warn("[DEBUG] #{msg}")
        def error(msg) = Kestowv::Core::Klog.error("[DEBUG] #{msg}")

        def to_a
          Kestowv::Core::Klog.recent(20)
        end

        def stats
          {
            feature:   :debug_log,
            klog_size: Kestowv::Core::Klog.to_a.size
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
  :debug_log,
  __FILE__,
  feature:    :debug_log,
  depends_on: [:core_klog]
)
