# frozen_string_literal: true

# Kestówv 0.5.1 - fs/file.rb
#
# File operations abstraction.
# Registers file features.

module Kestowv
  module Fs
    module File
      @files = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_file)
          Boot.set_bit(:fs_file)
        end

        # Avoid collision with Kernel#open
        def file_open(path, flags)
          @mutex.synchronize do
            @files[path] = { flags: flags, opened_at: Time.now }
          end
        end

        def file_close(path)
          @mutex.synchronize { @files.delete(path) }
        end

        def to_a
          @files.keys
        end

        def stats
          {
            feature:    :fs_file,
            open_count: @files.size
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
  :fs_file,
  __FILE__,
  feature:    :fs_file,
  depends_on: [:fs]
)
