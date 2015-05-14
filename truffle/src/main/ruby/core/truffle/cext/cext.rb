# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle

  module CExt

    # Are C extensions supported?
    def self.supported?
      Truffle::Primitive.cext_supported?
    end

    # Load a set of C source code files as a C extension, specifying
    # the name of the initialize functions and the C compiler flags. You can
    # use this to load multiple C extensions by supplying multiple init
    # functions.
    # @return [nil]
    #
    # # Example
    #
    # ```
    # Truffle::CExt::load_files(['Init_foo', 'Init_bar'], ['-Wall'], ['foo.c', 'bar.c'])
    # ```
    def self.load_files(init_functions, c_flags, files)
      raise 'C extensions not supported' unless supported?
      Truffle::Primitive.cext_load init_functions, c_flags, files
    end

    # Load C source code from a string, specifying the name of the
    # initialize function and the C compiler flags.
    # @return [nil]
    #
    # # Example
    #
    # ```
    # Truffle::CExt::load_string('Init_foo', ['-Wall'], %{
    #   #include <ruby.h>
    #
    #   VALUE add(VALUE self, VALUE a, VALUE b) {
    #     return INT2NUM(NUM2INT(a) + NUM2INT(b));
    #   }
    #
    #   void Init_foo() {
    #     VALUE Foo = rb_define_module("Foo");
    #     rb_define_method(Foo, "add", add, 2);
    #   }
    # })
    # ```
    def self.load_string(init_function, c_flags, string)
      temp_file = 'temp.c'

      File.open(temp_file, 'w') do |file|
        file.write(string)
      end

      begin
        load_files [init_function], c_flags, [temp_file]
      ensure
        File.delete temp_file
      end
    end

    # Load a C extension from the path of an extconf.rb file.
    # @return [nil]
    #
    # # Example
    #
    # ```
    # Truffle::CExt::load_extconf 'foo/ext/foo/extconf.rb'
    # ```
    def self.load_extconf(path)
      source = File.read(path)

      # Uses global variables - need to isolate somehow - maybe a
      # subprocess? Or some new Truffle-specific functionality for
      # mocking globals.

      $CFLAGS = []

      mkmf = MkMf.new(File.expand_path("..", path))
      mkmf.instance_eval(source)

      # Not sure we're handling $CFLAGS correctly - we get extra spaces in them
      cflags = $CFLAGS.map { |flag| flag.strip }

      load_files ["Init_#{mkmf.target_name}"], cflags, mkmf.c_files
    end

    # Load C source code from very simple inline code.
    # @return [nil]
    # 
    # ```
    # Truffle::CExt.inline %{
    #   #include <stdio.h>
    # }, %{
    #   printf("Hello, World!\\n");
    # }
    # ```
    def self.inline(headers, code, c_flags=[])
      load_string 'Init_inline', c_flags, "#include <ruby.h>\n #{headers}\n void Init_inline() { #{code} }\n"
    end

  end

end

