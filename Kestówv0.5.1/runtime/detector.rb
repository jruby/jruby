# frozen_string_literal: true

# Kestówv 0.5.1 - runtime/detector.rb
#
# Runtime detection and capability registration.
# Detects MRI / JRuby / TruffleRuby and registers capabilities as bit vector features.
# Uses only Boot primitives for any future loading needs.

module Kestowv
  module Runtime
    module Detector
      class << self
        def detect
          runtime = RUBY_ENGINE.to_sym

          Boot.register(:runtime_mri)
          Boot.register(:runtime_jruby)
          Boot.register(:runtime_truffleruby)

          case runtime
          when :ruby
            Boot.set_bit(:runtime_mri)
            @current = :mri
          when :jruby
            Boot.set_bit(:runtime_jruby)
            @current = :jruby
          when :truffleruby
            Boot.set_bit(:runtime_truffleruby)
            @current = :truffleruby
          else
            @current = :unknown
          end

          @current
        end

        def current
          @current || detect
        end

        def jruby?
          current == :jruby
        end

        def mri?
          current == :mri
        end

        def truffleruby?
          current == :truffleruby
        end

        def capabilities
          caps = []
          caps << :jruby_builtins if jruby? && defined?(JRuby)
          caps
        end

        def to_a
          {
            runtime:      current,
            jruby:        jruby?,
            mri:          mri?,
            truffleruby:  truffleruby?,
            capabilities: capabilities
          }
        end

        def stats
          {
            runtime:             current,
            features_registered: Boot.enabled_features.size
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
  :runtime_detector,
  __FILE__,
  feature:    :runtime_detector,
  depends_on: []
)
