# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Designed for ExecJS 2.6.0, but I'll allow it to try to work with anything for now

require 'execjs/runtime'
require 'json'

module ExecJS

  class TruffleRuntime < Runtime

    JS_MIME_TYPE = 'application/javascript'

    class Context < Runtime::Context

      STRINGIFY = Truffle::Interop.eval(JS_MIME_TYPE, 'JSON.stringify')
      PARSE = Truffle::Interop.eval(JS_MIME_TYPE, 'JSON.parse')

      def initialize(runtime, source = '')
        exec source
      end

      def exec(source, options = {})
        Truffle::Interop.eval JS_MIME_TYPE, source
        nil
      end

      def eval(source, options = {})
        unbox(Truffle::Interop.eval(JS_MIME_TYPE, source))
      end

      def call(identifier, *args)
        function = Truffle::Interop.eval(JS_MIME_TYPE, identifier)
        unbox(function.call(function, *args.map { |arg| box(arg) }))
      end

      private

      def unbox(value)
        if Truffle::Interop.boxed?(value)
          value
        else
          JSON.parse(Truffle::Interop.from_java_string(STRINGIFY.call(STRINGIFY, value)))
        end
      end

      def box(value)
        if Truffle::Interop.boxed?(value) || value.is_a?(String)
          value
        else
          PARSE.call(PARSE, JSON.generate(value))
        end
      end

    end

    def name
      'Truffle'
    end

    def available?
      Truffle::Interop.mime_type_supported?('application/javascript')
    end

  end

  # Monkey patches

  module Runtimes

    @runtimes = nil

    class << self

      alias_method :original_runtimes, :runtimes

      def runtimes
        @runtimes ||= ([TruffleRuntime.new] + original_runtimes)
      end

    end

  end

  self.runtime = Runtimes.autodetect

end
