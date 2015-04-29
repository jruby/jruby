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
    # the name of the initialize function and the C compiler flags.
    # @return [nil]
    #
    # # Example
    #
    # ```
    # Truffle::CExt::load_files(['Init_foo', 'Init_bar'], ['-Wall'], ['foo.c', 'bar.c'])
    # ```
    def self.load_files(init_functions, c_flags, files)
      raise 'C extensions not supported' unless supported?

      # This is a major hack. We can currently only load C code once. So to
      # support multiple C extensions being loaded, we delay loading them
      # until they've all been required. To do that we need to know how many
      # C extensions there will be. It's 1 by default, and you can override
      # with the $JRUBY_TRUFFLE_CEXT_DELAY variable.

      @delayed_count ||= 0
      @delayed_init_functions ||= []
      @delayed_cflags ||= []
      @delayed_files ||= []

      @delayed_count += 1
      @delayed_init_functions.concat(init_functions)
      @delayed_cflags.concat(c_flags)
      @delayed_files.concat(files)

      unless @delay_count
        if ENV.include? 'JRUBY_TRUFFLE_CEXT_DELAY'
          @delay_count = Integer(ENV['JRUBY_TRUFFLE_CEXT_DELAY'])
        else
          @delay_count = 1
        end
      end

      if @delayed_count > @delay_count
        raise "C extensions already loaded - can't load more - use $JRUBY_TRUFFLE_CEXT_DELAY"
      elsif @delayed_count == @delay_count
        Truffle::Primitive.cext_load @delayed_init_functions, @delayed_cflags, @delayed_files
      else
        # Do nothing - wait for more C extensions to be loaded
      end
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

