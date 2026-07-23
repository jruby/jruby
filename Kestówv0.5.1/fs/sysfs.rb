# frozen_string_literal: true

# Kestówv 0.5.1 - fs/sysfs.rb
#
# Sysfs simulation.
# Registers sysfs features.

module Kestowv
  module Fs
    module Sysfs
      @entries = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_sysfs)
          Boot.set_bit(:fs_sysfs)
        end

        def add_entry(path, content)
          @mutex.synchronize { @entries[path] = content }
        end

        def read(path)
          @entries[path]
        end

        def to_a
          @entries.keys
        end

        def stats
          {
            feature: :fs_sysfs,
            entries: @entries.size
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
  :fs_sysfs,
  __FILE__,
  feature:    :fs_sysfs,
  depends_on: [:fs]
)
