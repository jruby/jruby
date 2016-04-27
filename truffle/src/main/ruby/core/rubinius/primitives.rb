# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius
  module RubyPrimitives

    def self.module_mirror(obj)
      case obj
        when ::Numeric then Rubinius::Mirror::Numeric
        when ::String then Rubinius::Mirror::String
        when ::Range then Rubinius::Mirror::Range
        when ::Process then Rubinius::Mirror::Process
        else
          begin
            Rubinius::Mirror.const_get(obj.class.name.to_sym, false)
          rescue NameError
            ancestor = obj.class.superclass

            until ancestor.nil?
              begin
                return Rubinius::Mirror.const_get(ancestor.name.to_sym, false)
              rescue NameError
                ancestor = ancestor.superclass
              end
            end

            nil
          end
      end
    end

    Truffle.install_rubinius_primitive method(:module_mirror)

    if Truffle.substrate?

      def self.vm_gc_start(force)
        Truffle::Interop.execute(Truffle::Interop.read_property(Truffle::Java::System, :gc))
      end

      Truffle.install_rubinius_primitive method(:vm_gc_start)

    end

    def self.vm_spawn(options, command, arguments)
      options ||= {}
      env     = options[:unsetenv_others] ? {} : ENV.to_hash
      env.merge! Hash[options[:env]] if options[:env]

      env_array = env.map { |k, v| "#{k}=#{v}" }

      if arguments.empty?
        command, arguments = 'bash', ['bash', '-c', command]
      end

      Truffle.spawn_process command, arguments, env_array
    end

    Truffle.install_rubinius_primitive method(:vm_spawn)
  end
end
