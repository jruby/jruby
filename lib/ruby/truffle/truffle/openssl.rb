# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

if ENV['JRUBY_TRUFFLE_NATIVE_OPENSSL']
  require_relative '../openssl/openssl'
else
  # If loaded directly simulate as it was not found, it can added only
  # explicitly by loading openssl-stubs which makes it look like
  # openssl was loaded.

  load_error = LoadError.new("cannot load such file -- openssl")
  load_error.instance_variable_set :@path, 'openssl'
  raise load_error
end
