#
# Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
#
# JRuby - http://jruby.sourceforge.net
#
# This file is part of JRuby
#
# JRuby is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 2 of the
# License, or (at your option) any later version.
#
# JRuby is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public
# License along with JRuby; if not, write to
# the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
# Boston, MA  02111-1307 USA

require 'bcel.rb'

module JRuby
  module Compiler
    module Bytecode

      SELF_INDEX = 1
      RUNTIME_INDEX = 0

      IRUBYOBJECT_TYPE =
        BCEL::ObjectType.new("org.jruby.runtime.builtin.IRubyObject")
      RUBY_TYPE =
        BCEL::ObjectType.new("org.jruby.Ruby")

 
      class Instruction

        def push_runtime(generator)
          generator.append(BCEL::ALOAD.new(RUNTIME_INDEX))
        end

        def get_scope_stack(generator)
          push_runtime(generator)
          generator.appendInvoke(RUBY_TYPE.getClassName,
                                 "getScope",
                                 BCEL::ObjectType.new("org.jruby.runtime.ScopeStack"),
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)
        end

        def get_frame_stack(generator)
          push_runtime(generator)
          generator.appendInvoke(RUBY_TYPE.getClassName,
                                 "getFrameStack",
                                 BCEL::ObjectType.new("org.jruby.runtime.FrameStack"),
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)
        end
      end


      class CreateMethod < Instruction
        def initialize(name, arity)
          @name, @arity = name, arity
        end

        def emit_jvm_bytecode(generator)

          push_runtime(generator)
          generator.appendPush(generator.java_class_name)
          generator.appendPush(@name)
          generator.appendPush(@arity)

          arg_types = BCEL::Type[].new(4)
          arg_types[0] = BCEL::ObjectType.new("org.jruby.Ruby")
          arg_types[1] = BCEL::Type::STRING
          arg_types[2] = BCEL::Type::STRING
          arg_types[3] = BCEL::Type::INT
          generator.appendInvoke("org.jruby.compiler.ByteCodeRuntime",
                                 "registerMethod",
                                 BCEL::Type::VOID,
                                 arg_types,
                                 BCEL::Constants::INVOKESTATIC)
        end
      end

      class GetModule < Instruction
        def initialize(name)
          @name = name
        end

        def emit_jvm_bytecode(generator)
          push_runtime(generator)
          generator.appendPush(@name)
          arg_types = BCEL::Type[].new(1)
          arg_types[0] = BCEL::Type::STRING
          generator.appendInvoke("org.jruby.Ruby",
                                 "getModule",
                                 BCEL::ObjectType.new("org.jruby.RubyModule"),
                                 arg_types,
                                 BCEL::Constants::INVOKEVIRTUAL)
        end
      end

      class AssignConstant < Instruction
        def initialize(name)
          @name = name
        end

        def emit_jvm_bytecode(generator)
          # Stack: ..., value
          generator.append(BCEL::DUP.new)
          # Stack: ..., value, value
          push_runtime(generator)
          generator.appendInvoke("org.jruby.Ruby",
                                 "getRubyClass",
                                 BCEL::ObjectType.new("org.jruby.RubyModule"),
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)
          # Stack: ..., value, value, module
          generator.append(BCEL::SWAP.new)
          # Stack: ..., value, module, value
          generator.appendPush(@name)
          # Stack: ..., value, module, value, name
          generator.append(BCEL::SWAP.new)
          # Stack: ..., value, module, name, value
          arg_types = BCEL::Type[].new(2)
          arg_types[0] = BCEL::Type::STRING
          arg_types[1] = IRUBYOBJECT_TYPE
          generator.appendInvoke("org.jruby.RubyModule",
                                 "setConstant",
                                 BCEL::Type::VOID,
                                 arg_types,
                                 BCEL::Constants::INVOKEVIRTUAL)
          # Stack: ..., value
        end
      end

      class AssignLocal < Instruction
        attr_reader :index

        def initialize(index)
          @index = index
        end
      end

      class PushFixnum < Instruction
        attr_reader :value

        def initialize(value)
          @value = value
        end

        def emit_jvm_bytecode(generator)
          push_runtime(generator)
          generator.appendPush(@value)
          generator.append(BCEL::I2L.new())

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = RUBY_TYPE
          arg_types[1] = BCEL::Type::LONG
          generator.appendInvoke("org.jruby.RubyFixnum",
                                 "newFixnum",
                                 BCEL::ObjectType.new("org.jruby.RubyFixnum"),
                                 arg_types,
                                 BCEL::Constants::INVOKESTATIC)
        end
      end

      class PushSelf < Instruction

        def emit_jvm_bytecode(generator)
          generator.append(BCEL::ALOAD.new(SELF_INDEX))
        end
      end

      class PushLocal < Instruction
        def initialize(index)
          @index = index
        end

        def emit_jvm_bytecode(generator)
          get_scope_stack(generator) # ... why do we do this?
          generator.appendPush(@index)
          arg_types = BCEL::Type[].new(1)
          arg_types[0] = BCEL::Type::INT
          generator.appendInvoke("org.jruby.runtime.ScopeStack",
                                 "getValue",
                                 IRUBYOBJECT_TYPE,
                                 arg_types,
                                 BCEL::Constants::INVOKEVIRTUAL)
        end
      end

      class Call < Instruction
        attr_reader :name
        attr_reader :arity

        def initialize(name, arity, type)
          @name, @arity, @type = name, arity, type
        end

        def emit_jvm_bytecode(generator)
          args_array = generator.getLocalVariables.detect {|lv|
            lv.getName == "args_array"
          }
          if args_array.nil?
            args_array =
              generator.addLocalVariable("args_array",
                                         BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1),
                                         nil,
                                         nil)
          end

          factory = generator.factory

          generator.appendPush(@arity)
          generator.append(factory.createNewArray(IRUBYOBJECT_TYPE, 1))
          args_array.setStart(generator.getEnd())

          generator.append(BCEL::InstructionFactory.createStore(args_array.getType,
                                                                args_array.getIndex))

          # Take the method arguments from the stack
          # and put them in the array.
          for i in 0...arity
            generator.append(BCEL::InstructionFactory.createLoad(args_array.getType,
                                                                 args_array.getIndex))
            generator.append(BCEL::SWAP.new)
            generator.appendPush(i)
            generator.append(BCEL::SWAP.new)
            generator.append(BCEL::AASTORE.new())
          end

          generator.append(BCEL::InstructionFactory.createLoad(args_array.getType,
                                                          args_array.getIndex))
          generator.appendPush(@name)
          generator.append(BCEL::SWAP.new)

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = BCEL::Type::STRING
          arg_types[1] = BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1)
          generator.appendInvoke(IRUBYOBJECT_TYPE.getClassName,
                                 "callMethod",
                                 IRUBYOBJECT_TYPE,
                                 arg_types,
                                 BCEL::Constants::INVOKEINTERFACE)
        end
      end

      class PushString < Instruction
        attr_reader :value

        def initialize(value)
          @value = value
        end

        def emit_jvm_bytecode(generator)
          push_runtime(generator)
          generator.appendPush(@value)

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = RUBY_TYPE
          arg_types[1] = BCEL::Type::STRING
          generator.appendInvoke("org.jruby.RubyString",
                                 "newString",
                                 BCEL::ObjectType.new("org.jruby.RubyString"),
                                 arg_types,
                                 BCEL::Constants::INVOKESTATIC)
        end
      end

      class PushSymbol < Instruction
        def initialize(name)
          @name = name
        end

        def emit_jvm_bytecode(generator)
          push_runtime(generator)
          generator.appendPush(@name)

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = RUBY_TYPE
          arg_types[1] = BCEL::Type::STRING
          generator.appendInvoke("org.jruby.RubySymbol",
                                 "newSymbol",
                                 BCEL::ObjectType.new("org.jruby.RubySymbol"),
                                 arg_types,
                                 BCEL::Constants::INVOKESTATIC)
        end
      end

      class Negate  < Instruction

      end

      class PushNil < Instruction
        def emit_jvm_bytecode(generator)
          push_runtime(generator)
          generator.appendInvoke(RUBY_TYPE.getClassName,
                                 "getNil",
                                 IRUBYOBJECT_TYPE,
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)
        end
      end

      class PushBoolean < Instruction
        def initialize(value)
          @value = value
        end

        def emit_jvm_bytecode(generator)
          push_runtime(generator)
          if @value
            methodname = "getTrue"
          else
            methodname = "getFalse"
          end
          generator.appendInvoke(RUBY_TYPE.getClassName,
                                 methodname,
                                 BCEL::ObjectType.new("org.jruby.RubyBoolean"),
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)
        end
      end

      class PushArray < Instruction
        def initialize(initial_size)
          @size = initial_size
        end

        def emit_jvm_bytecode(generator)
          push_runtime(generator)
          generator.appendPush(@size)
          generator.append(BCEL::I2L.new)

          args_array = BCEL::Type[].new(2)
          args_array[0] = RUBY_TYPE
          args_array[1] = BCEL::Type::LONG
          generator.appendInvoke("org.jruby.RubyArray",
                                 "newArray",
                                 BCEL::ObjectType.new("org.jruby.RubyArray"),
                                 args_array,
                                 BCEL::Constants::INVOKESTATIC)
        end
      end

      class PushConstant < Instruction
        def initialize(name)
          @name = name
        end

        def emit_jvm_bytecode(generator)
          push_runtime(generator)
          generator.appendInvoke("org.jruby.Ruby",
                                 "getCurrentContext",
                                 BCEL::ObjectType.new("org.jruby.runtime.ThreadContext"),
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)
          generator.appendPush(@name)
          arg_types = BCEL::Type[].new(1)
          arg_types[0] = BCEL::Type::STRING
          generator.appendInvoke("org.jruby.runtime.ThreadContext",
                                 "getConstant",
                                 IRUBYOBJECT_TYPE,
                                 arg_types,
                                 BCEL::Constants::INVOKEVIRTUAL)
        end
      end

      class IfFalse < Instruction
        attr_writer :target

        def initialize(target)
          @target = target
        end

        def emit_jvm_bytecode(generator)
          generator.appendInvoke(IRUBYOBJECT_TYPE.getClassName,
                                 "isTrue",
                                 BCEL::Type::BOOLEAN,
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEINTERFACE)
          # If value on stack was false we should have a 0 now
          branch = BCEL::IFEQ.new(nil)
          @target.add_listener(branch)
          generator.append(branch)
        end
      end

      class NewScope < Instruction
        def initialize(local_names)
          @local_names = local_names
        end

        def emit_jvm_bytecode(generator)
          # runtime.getFrameStack().pushCopy()
          get_frame_stack(generator)
          generator.appendInvoke("org.jruby.runtime.FrameStack",
                                 "pushCopy",
                                 BCEL::Type::VOID,
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)

          # runtime.getScopeStack().push(localnames)
          get_scope_stack(generator)
          # FIXME: store this array instead of creating it on the fly!
          generator.appendPush(@local_names.size)
          factory = generator.factory
          generator.append(factory.createNewArray(BCEL::Type::STRING, 1))

          iter = @local_names.iterator
          index = 0
          while iter.hasNext
            generator.append(BCEL::DUP.new)
            name = iter.next
            generator.appendPush(index)
            index += 1
            generator.appendPush(name)
            generator.append(BCEL::AASTORE.new)
          end
          # Stack: ..., scopestack, namesarray
          arg_types = BCEL::Type[].new(1)
          arg_types[0] = BCEL::ArrayType.new(BCEL::Type::STRING, 1)
          generator.appendInvoke("org.jruby.runtime.ScopeStack",
                                 "push",
                                 BCEL::Type::VOID,
                                 arg_types,
                                 BCEL::Constants::INVOKEVIRTUAL)

          # Set up Ruby locals (ignore _ and ~ vars for now)
          for i in 2...@local_names.size
            get_scope_stack(generator)
            generator.appendPush(i)
            generator.append(BCEL::ALOAD.new(i))

            arg_types = BCEL::Type[].new(2)
            arg_types[0] = BCEL::Type::INT
            arg_types[1] = IRUBYOBJECT_TYPE
            generator.appendInvoke("org.jruby.runtime.ScopeStack",
                                   "setValue",
                                   BCEL::Type::VOID,
                                   arg_types,
                                   BCEL::Constants::INVOKEVIRTUAL)
          end
        end
      end

      class RestoreScope < Instruction
        def emit_jvm_bytecode(generator)
          # getScopeStack.pop()
          get_scope_stack(generator)
          generator.appendInvoke("org.jruby.runtime.ScopeStack",
                                 "pop",
                                 BCEL::ObjectType.new("org.jruby.util.collections.StackElement"),
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)
          generator.append(BCEL::POP.new)
          # getFrameStack.pop()
          get_frame_stack(generator)
          generator.appendInvoke("org.jruby.runtime.FrameStack",
                                 "pop",
                                 BCEL::ObjectType.new("java.lang.Object"),
                                 BCEL::Type[].new(0),
                                 BCEL::Constants::INVOKEVIRTUAL)
          generator.append(BCEL::POP.new)
        end
      end

      class CreateRange < Instruction
        def initialize(exclusive)
          @exclusive = exclusive
        end

        def emit_jvm_bytecode(generator)

          # Inserting 'runtime' before the two range arguments

          # Stack: ..., begin, end
          push_runtime(generator)
          # Stack: ..., begin, end, runtime
          generator.append(BCEL::DUP_X2.new)
          # Stack: ..., runtime, begin, end, runtime
          generator.append(BCEL::POP.new)
          # Stack: ..., runtime, begin, end
          generator.appendPush(@exclusive)
          # Stack: ..., runtime, begin, end, isexclusive

          arg_types = BCEL::Type[].new(4)
          arg_types[0] = RUBY_TYPE
          arg_types[1] = IRUBYOBJECT_TYPE
          arg_types[2] = IRUBYOBJECT_TYPE
          arg_types[3] = BCEL::Type::BOOLEAN
          generator.appendInvoke("org.jruby.RubyRange",
                                 "newRange",
                                 BCEL::ObjectType.new("org.jruby.RubyRange"),
                                 arg_types,
                                 BCEL::Constants::INVOKESTATIC)
        end
      end

      class Return < Instruction
        def emit_jvm_bytecode(generator)
          # FIXME: instead throw new ReturnJump(value) ..?
#          generator.append(ARETURN.new)
          generator.append(BCEL::ARETURN.new)
        end
      end

      class Goto < Instruction
        attr_writer :target

        def initialize(target)
          @target = target
        end

        def emit_jvm_bytecode(generator)
          goto = BCEL::GOTO.new(nil)
          @target.add_listener(goto)
          generator.append(goto)
        end
      end

      class Label < Instruction
        def initialize
          @handle = nil
          @listeners = []
        end

        def add_listener(listener)
          @listeners << listener
          unless @handle.nil?
            listener.setTarget(@handle)
          end
        end

        def emit_jvm_bytecode(generator)
          @handle = generator.append(BCEL::NOP.new)
          @listeners.each {|l| l.setTarget(@handle) }
        end
      end

      class Dup < Instruction
        def emit_jvm_bytecode(generator)
          generator.append(BCEL::DUP.new)
        end
      end

      class Pop < Instruction
        def emit_jvm_bytecode(generator)
          generator.append(BCEL::POP.new)
        end
      end

    end
  end
end
