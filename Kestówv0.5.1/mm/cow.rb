# frozen_string_literal: true

# Kestówv 0.5.1 - mm/cow.rb
#
# Copy-on-Write simulation.
# Registers COW features.

module Kestowv
  module Mm
    module Cow
      @pages = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:mm_cow)
          Boot.set_bit(:mm_cow)
        end

        def mark_cow(pid, vaddr)
          @mutex.synchronize { @pages[[pid, vaddr]] = true }
        end

        def is_cow?(pid, vaddr)
          @pages[[pid, vaddr]] == true
        end

        def to_a
          @pages.keys
        end

        def stats
          {
            feature: :mm_cow,
            pages:   @pages.size
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
  :mm_cow,
  __FILE__,
  feature:    :mm_cow,
  depends_on: [:mm_page]
)
