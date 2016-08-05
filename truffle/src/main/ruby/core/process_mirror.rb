# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

module Rubinius
  class Mirror
    module Process
      def self.set_status_global(status)
        $? = status
      end

      def self.exec(*args)
        exe = Execute.new(*args)
        exe.spawn_setup
        exe.exec exe.command, exe.argv
      end

      def self.spawn(*args)
        exe = Execute.new(*args) 

        begin
          pid = exe.spawn exe.options, exe.command, exe.argv
        rescue SystemCallError => error
          set_status_global ::Process::Status.new(pid, 127)
          raise error
        end

        pid
      end

      class Execute
        attr_reader :command
        attr_reader :argv
        attr_reader :options

        # Turns the various varargs incantations supported by Process.spawn into a
        # [env, prog, argv, redirects, options] tuple.
        #
        # The following method signature is supported:
        #   Process.spawn([env], command, ..., [options])
        #
        # The env and options hashes are optional. The command may be a variable
        # number of strings or an Array full of strings that make up the new process's
        # argv.
        #
        # Assigns @environment, @command, @argv, @redirects, @options. All
        # elements are guaranteed to be non-nil. When no env or options are
        # given, empty hashes are returned.
        def initialize(env_or_cmd, *args)
          if options = Rubinius::Type.try_convert(args.last, ::Hash, :to_hash)
            args.pop
          end

          if env = Rubinius::Type.try_convert(env_or_cmd, ::Hash, :to_hash)
            unless command = args.shift
              raise ArgumentError, "command argument expected"
            end
          else
            command = env_or_cmd
          end

          if args.empty? and
              cmd = Rubinius::Type.try_convert(command, ::String, :to_str)
            raise Errno::ENOENT if cmd.empty?

            @command = cmd
            @argv = []
          else
            if cmd = Rubinius::Type.try_convert(command, ::Array, :to_ary)
              raise ArgumentError, "wrong first argument" unless cmd.size == 2
              command = StringValue(cmd[0])
              name = StringValue(cmd[1])
            else
              name = command = StringValue(command)
            end

            argv = [name]
            args.each { |arg| argv << StringValue(arg) }

            @command = command
            @argv = argv
          end

          @options = Rubinius::LookupTable.new if options or env

          if options
            options.each do |key, value|
              case key
              when ::IO, ::Fixnum, :in, :out, :err
                from = convert_io_fd key
                to = convert_to_fd value, from
                redirect @options, from, to
              when ::Array
                from = convert_io_fd key.first
                to = convert_to_fd value, from
                key.each { |k| redirect @options, convert_io_fd(k), to }
              when :unsetenv_others
                if value
                  @options[:unsetenv_others] = true
                end
              when :pgroup
                if value == true
                  value = 0
                elsif value
                  value = Rubinius::Type.coerce_to value, ::Integer, :to_int
                  raise ArgumentError, "negative process group ID : #{value}" if value < 0
                end
                @options[key] = value
              when :chdir
                @options[key] = Rubinius::Type.coerce_to_path(value)
              when :umask
                @options[key] = value
              when :close_others
                @options[key] = value if value
              else
                raise ArgumentError, "unknown exec option: #{key.inspect}"
              end
            end
          end

          if env
            array = (@options[:env] ||= [])

            env.each do |key, value|
              array << [convert_env_key(key), convert_env_value(value)]
            end
          end
        end

        def redirect(options, from, to)
          case to
          when ::Fixnum
            map = (options[:redirect_fd] ||= [])
            map << from << to
          when ::Array
            map = (options[:assign_fd] ||= [])
            map << from
            map.concat to
          end
        end

        def convert_io_fd(obj)
          case obj
          when ::Fixnum
            obj
          when :in
            0
          when :out
            1
          when :err
            2
          when ::IO
            obj.fileno
          else
            raise ArgementError, "wrong exec option: #{obj.inspect}"
          end
        end

        def convert_to_fd(obj, target)
          case obj
          when ::Fixnum
            obj
          when :in
            0
          when :out
            1
          when :err
            2
          when :close
            nil
          when ::IO
            obj.fileno
          when ::String
            [obj, default_mode(target), 0644]
          when ::Array
            case obj.size
            when 1
              [obj[0], File::RDONLY, 0644]
            when 2
              if obj[0] == :child
                fd = convert_to_fd obj[1], target
                fd.kind_of?(::Fixnum) ?  -(fd + 1) : fd
              else
                [obj[0], convert_file_mode(obj[1]), 0644]
              end
            when 3
              [obj[0], convert_file_mode(obj[1]), obj[2]]
            end
          else
            raise ArgumentError, "wrong exec redirect: #{obj.inspect}"
          end
        end

        def default_mode(target)
          if target == 1 or target == 2
            OFLAGS["w"]
          else
            OFLAGS["r"]
          end
        end

        def convert_file_mode(obj)
          case obj
          when ::Fixnum
            obj
          when ::String
            OFLAGS[obj]
          when nil
            OFLAGS["r"]
          else
            Rubinius::Type.coerce_to obj, Integer, :to_int
          end
        end

        def convert_env_key(key)
          key = Rubinius::Type.check_null_safe(StringValue(key))

          if key.include?("=")
            raise ArgumentError, "environment name contains a equal : #{key}"
          end

          key
        end

        def convert_env_value(value)
          return if value.nil?
          Rubinius::Type.check_null_safe(StringValue(value))
        end

        # Mapping of string open modes to integer oflag versions.
        OFLAGS = {
          "r"  => ::File::RDONLY,
          "r+" => ::File::RDWR   | ::File::CREAT,
          "w"  => ::File::WRONLY | ::File::CREAT  | ::File::TRUNC,
          "w+" => ::File::RDWR   | ::File::CREAT  | ::File::TRUNC,
          "a"  => ::File::WRONLY | ::File::APPEND | ::File::CREAT,
          "a+" => ::File::RDWR   | ::File::APPEND | ::File::CREAT
        }

        def spawn(options, command, arguments)
          options ||= {}
          env = options.delete(:unsetenv_others) ? {} : ENV.to_hash
          if add_to_env = options.delete(:env)
            env.merge! Hash[add_to_env]
          end

          env_array = env.map { |k, v| "#{k}=#{v}" }

          if arguments.empty?
            command, arguments = 'bash', ['bash', '-c', command]
          end

          Truffle::Process.spawn command, arguments, env_array, options
        end

        def exec(command, args)
          Truffle.primitive :vm_exec
          raise PrimitiveFailure,
            "Rubinius::Mirror::Process::Execute#exec primitive failed"
        end
      end
    end
  end
end
