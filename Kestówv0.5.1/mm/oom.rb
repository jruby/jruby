# frozen_string_literal: true

# Kestówv 0.5.1 - mm/oom.rb
#
# Out-of-memory handling simulation.
# Registers OOM features.

module Kestowv
  module Mm
    module Oom
      @score = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:mm_oom)
          Boot.set_bit(:mm_oom)
        end

        def set_score(pid, score)
          @mutex.synchronize { @score[pid] = score }
        end

        def get_score(pid)
          @score[pid] || 0
        end

        def to_a
          @score
        end

        def stats
          {
            feature: :mm_oom,
            tracked: @score.size
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
  :mm_oom,
  __FILE__,
  feature:    :mm_oom,
  depends_on: [:mm_physical]
)
