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

      class AssignLocal
        attr_reader :index

        def initialize(index)
          @index = index
        end
      end

      class PushFixnum
        attr_reader :value

        def initialize(value)
          @value = value
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList

          push_runtime(methodgen)

          list.append(BCEL::PUSH.new(methodgen.getConstantPool, @value))
          list.append(BCEL::I2L.new())

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = RUBY_TYPE
          arg_types[1] = BCEL::Type::LONG
          list.append(factory.createInvoke("org.jruby.RubyFixnum",
                                           "newFixnum",
                                           BCEL::ObjectType.new("org.jruby.RubyFixnum"),
                                           arg_types,
                                           BCEL::Constants::INVOKESTATIC))
        end
      end

      class PushSelf

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList
          list.append(BCEL::ALOAD.new(SELF_INDEX))
        end
      end

      class PushLocal
        def initialize(index)
          @index = index
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList
          pushScopeStack(methodgen, factory)
          list.append(BCEL::PUSH.new(methodgen.getConstantPool,
                                     @index))
          arg_types = BCEL::Type[].new(1)
          arg_types[0] = BCEL::Type::INT
          list.append(factory.createInvoke("org.jruby.runtime.ScopeStack",
                                           "getValue",
                                           IRUBYOBJECT_TYPE,
                                           arg_types,
                                           BCEL::Constants::INVOKEVIRTUAL))
        end
      end

      def push_runtime(methodgen)
        list = methodgen.getInstructionList
        list.append(BCEL::ALOAD.new(RUNTIME_INDEX))
      end

      def push_scope_stack(methodgen, factory)
        list = methodgen.getInstructionList
        push_runtime(methodgen)
        list.append(factory.createInvoke(RUBY_TYPE.getClassName,
                                         "getScope",
                                         BCEL::ObjectType.new("org.jruby.runtime.ScopeStack"),
                                         BCEL::Type[].new(0),
                                         BCEL::Constants::INVOKEVIRTUAL))
      end

      def push_frame_stack(methodgen, factory)
        list = methodgen.getInstructionList
        push_runtime(methodgen)
        list.append(factory.createInvoke(RUBY_TYPE.getClassName,
                                         "getFrameStack",
                                         BCEL::ObjectType.new("org.jruby.runtime.FrameStack"),
                                         BCEL::Type[].new(0),
                                         BCEL::Constants::INVOKEVIRTUAL))
      end

      class Call
        attr_reader :name
        attr_reader :arity

        def initialize(name, arity, type)
          @name, @arity, @type = name, arity, type
        end

        def emit_jvm_bytecode(methodgen, factory)
          args_array = methodgen.getLocalVariables.detect {|lv|
            lv.getName == "args_array"
          }
          if args_array.nil?
            args_array =
              methodgen.addLocalVariable("args_array",
                                         BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1),
                                         nil,
                                         nil)
          end

          list = methodgen.getInstructionList


          list.append(BCEL::PUSH.new(methodgen.getConstantPool,
                                     @arity))
          list.append(factory.createNewArray(IRUBYOBJECT_TYPE, 1))
          args_array.setStart(list.getEnd())

          list.append(BCEL::InstructionFactory.createStore(args_array.getType,
                                                           args_array.getIndex))

          # Take the method arguments from the stack
          # and put them in the array.
          for i in 0...arity
            list.append(BCEL::InstructionFactory.createLoad(args_array.getType,
                                                            args_array.getIndex))
            list.append(BCEL::SWAP.new)
            list.append(BCEL::PUSH.new(methodgen.getConstantPool, i))
            list.append(BCEL::SWAP.new)
            list.append(BCEL::AASTORE.new())
          end

          list.append(BCEL::InstructionFactory.createLoad(args_array.getType,
                                                          args_array.getIndex))
          list.append(BCEL::PUSH.new(factory.getConstantPool, @name))
          list.append(BCEL::SWAP.new)

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = BCEL::Type::STRING
          arg_types[1] = BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1)
          list.append(factory.createInvoke(IRUBYOBJECT_TYPE.getClassName,
                                           "callMethod",
                                           IRUBYOBJECT_TYPE,
                                           arg_types,
                                           BCEL::Constants::INVOKEINTERFACE))
        end
      end

      class PushString
        attr_reader :value

        def initialize(value)
          @value = value
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList
          push_runtime(methodgen)
          list.append(BCEL::PUSH.new(factory.getConstantPool, @value))

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = RUBY_TYPE
          arg_types[1] = BCEL::Type::STRING
          list.append(factory.createInvoke("org.jruby.RubyString",
                                           "newString",
                                           BCEL::ObjectType.new("org.jruby.RubyString"),
                                           arg_types,
                                           BCEL::Constants::INVOKESTATIC))
        end
      end

      class PushSymbol
        def initialize(name)
          @name = name
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList
          push_runtime(methodgen)
          list.append(BCEL::PUSH.new(factory.getConstantPool, @name))

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = RUBY_TYPE
          arg_types[1] = BCEL::Type::STRING
          list.append(factory.createInvoke("org.jruby.RubySymbol",
                                           "newSymbol",
                                           BCEL::ObjectType.new("org.jruby.RubySymbol"),
                                           arg_types,
                                           BCEL::Constants::INVOKESTATIC))
        end
      end

      class Negate

      end

      class PushNil
        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList
          push_runtime(methodgen)
          list.append(factory.createInvoke(RUBY_TYPE.getClassName,
                                           "getNil",
                                           IRUBYOBJECT_TYPE,
                                           BCEL::Type[].new(0),
                                           BCEL::Constants::INVOKEVIRTUAL))
        end
      end

      class PushBoolean
        def initialize(value)
          @value = value
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList

          push_runtime(methodgen)
          if @value
            methodname = "getTrue"
          else
            methodname = "getFalse"
          end
          list.append(factory.createInvoke(RUBY_TYPE.getClassName,
                                           methodname,
                                           BCEL::ObjectType.new("org.jruby.RubyBoolean"),
                                           BCEL::Type[].new(0),
                                           BCEL::Constants::INVOKEVIRTUAL))
        end
      end

      class PushArray
        def initialize(initial_size)
          @size = initial_size
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList
          push_runtime(methodgen)
          list.append(BCEL::PUSH.new(methodgen.getConstantPool, @size))
          list.append(BCEL::I2L.new)

          args_array = BCEL::Type[].new(2)
          args_array[0] = RUBY_TYPE
          args_array[1] = BCEL::Type::LONG
          list.append(factory.createInvoke("org.jruby.RubyArray",
                                           "newArray",
                                           BCEL::ObjectType.new("org.jruby.RubyArray"),
                                           args_array,
                                           BCEL::Constants::INVOKESTATIC))
        end
      end

      class IfFalse
        attr_writer :target

        def initialize(target)
          @target = target
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList

          list.append(factory.createInvoke(IRUBYOBJECT_TYPE.getClassName,
                                           "isTrue",
                                           BCEL::Type::BOOLEAN,
                                           BCEL::Type[].new(0),
                                           BCEL::Constants::INVOKEINTERFACE))
          # If value on stack was false we should have a 0 now
          branch = BCEL::IFEQ.new(nil)
          @target.add_listener(branch)

          # list.append(branch)
          JavaClass.for_name("org.apache.bcel.generic.InstructionList").java_method(:append, "org.apache.bcel.generic.BranchInstruction").invoke(list.java_object, branch.java_object)

        end
      end

      class NewScope
        def initialize(local_names)
          @local_names = local_names
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList

          # runtime.getFrameStack().pushCopy()
          push_frame_stack(methodgen, factory)
          list.append(factory.createInvoke("org.jruby.runtime.FrameStack",
                                           "pushCopy",
                                           BCEL::Type::VOID,
                                           BCEL::Type[].new(0),
                                           BCEL::Constants::INVOKEVIRTUAL))

          # runtime.getScopeStack().push(localnames)
          push_scope_stack(methodgen, factory)
          # FIXME: store this array instead of creating it on the fly!
          list.append(BCEL::PUSH.new(methodgen.getConstantPool,
                                     @local_names.size))
          list.append(factory.createNewArray(BCEL::Type::STRING, 1))

          iter = @local_names.iterator
          index = 0
          while iter.hasNext
            list.append(BCEL::DUP.new)
            name = iter.next
            list.append(BCEL::PUSH.new(methodgen.getConstantPool,
                                       index))
            index += 1
            list.append(BCEL::PUSH.new(methodgen.getConstantPool,
                                       name))
            list.append(BCEL::AASTORE.new)
          end
          # Stack: ..., scopestack, namesarray
          arg_types = BCEL::Type[].new(1)
          arg_types[0] = BCEL::ArrayType.new(BCEL::Type::STRING, 1)
          list.append(factory.createInvoke("org.jruby.runtime.ScopeStack",
                                           "push",
                                           BCEL::Type::VOID,
                                           arg_types,
                                           BCEL::Constants::INVOKEVIRTUAL))
        end
      end

      class RestoreScope
        def emit_jvm_bytecode(methodgen, factory)
          # getScopeStack.pop()
          push_scope_stack(methodgen, factory)
          list = methodgen.getInstructionList
          list.append(factory.createInvoke("org.jruby.runtime.ScopeStack",
                                           "pop",
                                           BCEL::ObjectType.new("org.jruby.util.collections.StackElement"),
                                           BCEL::Type[].new(0),
                                           BCEL::Constants::INVOKEVIRTUAL))
          # getFrameStack.pop()
          push_frame_stack(methodgen, factory)
          list.append(factory.createInvoke("org.jruby.runtime.FrameStack",
                                           "pop",
                                           BCEL::ObjectType.new("java.lang.Object"),
                                           BCEL::Type[].new(0),
                                           BCEL::Constants::INVOKEVIRTUAL))
        end
      end

      class CreateRange
        def initialize(exclusive)
          @exclusive = exclusive
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList

          # Inserting 'runtime' before the two range arguments

          # Stack: ..., begin, end
          push_runtime(methodgen)
          # Stack: ..., begin, end, runtime
          list.append(BCEL::DUP_X2.new)
          # Stack: ..., runtime, begin, end, runtime
          list.append(BCEL::POP.new)
          # Stack: ..., runtime, begin, end
          list.append(BCEL::PUSH.new(methodgen.getConstantPool,
                                     @exclusive))
          # Stack: ..., runtime, begin, end, isexclusive

          arg_types = BCEL::Type[].new(4)
          arg_types[0] = RUBY_TYPE
          arg_types[1] = IRUBYOBJECT_TYPE
          arg_types[2] = IRUBYOBJECT_TYPE
          arg_types[3] = BCEL::Type::BOOLEAN
          list.append(factory.createInvoke("org.jruby.RubyRange",
                                           "newRange",
                                           BCEL::ObjectType.new("org.jruby.RubyRange"),
                                           arg_types,
                                           BCEL::Constants::INVOKESTATIC))
        end
      end

      class Goto
        attr_writer :target

        def initialize(target)
          @target = target
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList
          goto = BCEL::GOTO.new(nil)
          @target.add_listener(goto)

          #list.append(goto)
          JavaClass.for_name("org.apache.bcel.generic.InstructionList").java_method(:append, "org.apache.bcel.generic.BranchInstruction").invoke(list.java_object, goto.java_object)

        end
      end

      class Label
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

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList
          @handle = list.append(BCEL::NOP.new)
          @listeners.each {|l| l.setTarget(@handle) }
        end
      end

    end
  end
end
