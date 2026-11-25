# frozen_string_literal: true

# Kestówv 0.5.1 - fs/memfs.rb
#
# In-memory filesystem implementation.
# Registers memfs features.

module Kestowv
  module Fs
    module Memfs
      @files = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_memfs)
          Boot.set_bit(:fs_memfs)
        end

        def create(path, data = "")
          @mutex.synchronize { @files[path] = data }
        end

        def read(path)
          @files[path]
        end

        def write(path, data)
          @mutex.synchronize { @files[path] = data }
        end

        def to_a
          @files.keys
        end

        def stats
          {
            feature: :fs_memfs,
            files:   @files.size
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
  :fs_memfs,
  __FILE__,
  feature:    :fs_memfs,
  depends_on: [:fs]
)
