# frozen_string_literal: true

# Kestówv 0.5.1 - fs/dir.rb
#
# Directory operations.
# Registers directory features.

module Kestowv
  module Fs
    module Dir
      @dirs  = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_dir)
          Boot.set_bit(:fs_dir)
        end

        # Avoid collision with Ruby's ::Dir
        def dir_create(path)
          @mutex.synchronize do
            @dirs[path] = { created_at: Time.now }
          end
        end

        def dir_list(path)
          # Placeholder — real impl walks dentries
          @dirs.keys.select { |p| p.start_with?(path) }
        end

        def to_a
          @dirs.keys
        end

        def stats
          {
            feature: :fs_dir,
            count:   @dirs.size
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
  :fs_dir,
  __FILE__,
  feature:    :fs_dir,
  depends_on: [:fs]
)
