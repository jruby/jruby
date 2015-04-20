# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle

  module CExt

    # MkMf is the DSL to create Makefiles for C extensions. A basic
    # 'extconf.rb' file will require 'mkmf', set some $CFLAGS and then
    # call create_makefile. We run that 'extconf.rb' in an #instance_eval
    # using an instance of this class. See Truffle::CExt.load_extconf.

    class MkMf

      attr_reader :target_name
      attr_reader :c_files

      def initialize(directory)
        @directory = directory
        @c_files = []
      end

      # We're mocking mkmf so don't actually load it

      def require(path)
        super.require unless path == 'mkmf'
      end

      def create_makefile(target)
        @target_name = target.split('/').last
        @c_files = Dir.glob(File.join(@directory, '**', '*.c'))
      end

    end

  end

end
