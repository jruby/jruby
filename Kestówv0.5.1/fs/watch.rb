# frozen_string_literal: true

# Kestówv 0.5.1 - fs/watch.rb
#
# Filesystem watch/inotify simulation.
# Registers watch features.

module Kestowv
  module Fs
    module Watch
      @watches = {}
      @mutex   = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_watch)
          Boot.set_bit(:fs_watch)
        end

        def add_watch(path, &block)
          @mutex.synchronize { @watches[path] = block }
        end

        def notify(path)
          @watches[path]&.call(path)
        end

        def to_a
          @watches.keys
        end

        def stats
          {
            feature: :fs_watch,
            watches: @watches.size
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
  :fs_watch,
  __FILE__,
  feature:    :fs_watch,
  depends_on: [:fs]
)
