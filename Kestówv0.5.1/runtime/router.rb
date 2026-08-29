# frozen_string_literal: true

# Kestówv 0.5.1 - runtime/router.rb
#
# Routes tasks/subsystems based on detected runtime capabilities.
# Uses bit vector features from Detector.

module Kestowv
  module Runtime
    module Router
      class << self
        def route(task_type)
          case Kestowv::Runtime::Detector.current
          when :jruby then route_jruby(task_type)
          when :mri   then route_mri(task_type)
          else             route_generic(task_type)
          end
        end

        def route_jruby(task_type)
          case task_type
          when :compute then :jruby_thread_pool
          when :io      then :jruby_nio
          else               :generic
          end
        end

        def route_mri(task_type)
          :generic
        end

        def route_generic(task_type)
          :generic
        end

        def preferred_executor
          Kestowv::Runtime::Detector.jruby? ? :jruby_fiber : :thread
        end

        def to_a
          {
            runtime:            Kestowv::Runtime::Detector.current,
            preferred_executor: preferred_executor
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
  :runtime_router,
  __FILE__,
  feature:    :runtime_router,
  depends_on: [:runtime_detector]
)
