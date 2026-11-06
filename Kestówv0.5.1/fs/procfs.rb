# frozen_string_literal: true

# Kestówv 0.5.1 - fs/procfs.rb
#
# Procfs simulation.
# Registers procfs features.

module Kestowv
  module Fs
    module Procfs
      @entries = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_procfs)
          Boot.set_bit(:fs_procfs)
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
            feature: :fs_procfs,
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
  :fs_procfs,
  __FILE__,
  feature:    :fs_procfs,
  depends_on: [:fs]
)
