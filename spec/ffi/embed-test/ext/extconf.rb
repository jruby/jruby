#!/usr/bin/env ruby
#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

if RUBY_ENGINE == 'ruby' || RUBY_ENGINE == 'rbx'
  require "mkmf"

  create_makefile("embed_test")
end
