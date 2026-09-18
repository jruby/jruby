# frozen_string_literal: true

# Kestówv 0.5.1 - fs/dentry.rb
#
# Directory entry management.
# Registers dentry features.

module Kestowv
  module Fs
    module Dentry
      @dentries = {}
      @mutex    = Mutex.new

      class << self
        def register_features
          Boot.register(:fs_dentry)
          Boot.set_bit(:fs_dentry)
        end

        def create(parent, name, inode)
          @mutex.synchronize do
            @dentries[[parent, name]] = {
              inode:      inode,
              created_at: Time.now
            }
          end
        end

        def lookup(parent, name)
          @dentries[[parent, name]]
        end

        def to_a
          @dentries
        end

        def stats
          {
            feature: :fs_dentry,
            count:   @dentries.size
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
  :fs_dentry,
  __FILE__,
  feature:    :fs_dentry,
  depends_on: [:fs]
)
