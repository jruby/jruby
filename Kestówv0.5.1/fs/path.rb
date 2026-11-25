# frozen_string_literal: true

# Kestówv 0.5.1 - fs/path.rb
#
# Path utilities.
# Registers path features.

module Kestowv
  module Fs
    module Path
      class << self
        def register_features
          Boot.register(:fs_path)
          Boot.set_bit(:fs_path)
        end

        # Avoid collision with ::File.join — scoped here to Kestowv paths
        def join(*parts)
          parts.join("/")
        end

        def basename(path)
          path.split("/").last
        end

        def dirname(path)
          path.split("/")[0..-2].join("/")
        end

        def to_a
          { feature: :fs_path }
        end

        def stats
          { feature: :fs_path }
        end
      end
    end
  end
end
# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :fs_path,
  __FILE__,
  feature:    :fs_path,
  depends_on: [:fs]
)
