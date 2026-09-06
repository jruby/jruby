# frozen_string_literal: true

# Kestówv 0.5.1 - fs/attr.rb
#
# File attribute management.
# Registers attribute features.

module Kestowv
  module Fs
    module Attr
      @attrs = {}
      @mutex = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_attr)
          Boot.set_bit(:fs_attr)
        end

        def set(ino, key, value)
          @mutex.synchronize do
            @attrs[ino] ||= {}
            @attrs[ino][key] = value
          end
        end

        def get(ino, key)
          @attrs.dig(ino, key)
        end

        def to_a
          @attrs
        end

        def stats
          {
            feature: :fs_attr,
            inodes:  @attrs.size
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
  :fs_attr,
  __FILE__,
  feature:    :fs_attr,
  depends_on: [:fs]
)
