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

          list.append(BCEL::ALOAD.new(RUNTIME_INDEX))

          list.append(BCEL::ICONST.new(@value))
          list.append(BCEL::I2L.new())

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = BCEL::ObjectType.new("org.jruby.Ruby")
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

      class Call
        attr_reader :name
        attr_reader :arity

        def initialize(name, arity, type)
          @name, @arity, @type = name, arity, type
        end

        def emit_jvm_bytecode(methodgen, factory)

          args_array_variable =
            methodgen.addLocalVariable("args_array",
                                       BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1),
                                       nil,
                                       nil)

          list = methodgen.getInstructionList

          # ..., receiver, arg1, arg2

          list.append(BCEL::ICONST.new(@arity))
          list.append(factory.createNewArray(IRUBYOBJECT_TYPE, 1))

          # ..., receiver, arg1, arg2, args_array

          args_array_variable.setStart(list.getEnd())

          list.append(BCEL::InstructionFactory.createStore(BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1),
                                                           args_array_variable.getIndex))

          for i in 0...arity
            list.append(BCEL::InstructionFactory.createLoad(BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1),
                                                            args_array_variable.getIndex))
            # ..., receiver, arg1, ..., argN, args_array
            list.append(BCEL::SWAP.new)
            # ..., receiver, arg1, ..., args_array, argN
            list.append(BCEL::ICONST.new(i))
            # ..., receiver, arg1, ..., args_array, argN, index
            list.append(BCEL::SWAP.new)
            # ..., receiver, arg1, ..., args_array, index, argN
            list.append(BCEL::AASTORE.new())
            # ..., receiver, arg1, ..., argN-1
          end

          # ..., receiver
          list.append(BCEL::InstructionFactory.createLoad(BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1),
                                                          args_array_variable.getIndex))
          # ..., receiver, args_array

          list.append(BCEL::PUSH.new(factory.getConstantPool, @name))

          # ..., receiver, args_array, name

          list.append(BCEL::SWAP.new)

          # ..., receiver, name, args_array

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = BCEL::Type::STRING
          arg_types[1] = BCEL::ArrayType.new(IRUBYOBJECT_TYPE, 1)
          list.append(factory.createInvoke("org.jruby.runtime.builtin.IRubyObject",
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
          list.append(BCEL::ALOAD.new(RUNTIME_INDEX))
          list.append(BCEL::PUSH.new(factory.getConstantPool, @value))

          arg_types = BCEL::Type[].new(2)
          arg_types[0] = BCEL::ObjectType.new("org.jruby.Ruby")
          arg_types[1] = BCEL::Type::STRING
          list.append(factory.createInvoke("org.jruby.RubyString",
                                           "newString",
                                           BCEL::ObjectType.new("org.jruby.RubyString"),
                                           arg_types,
                                           BCEL::Constants::INVOKESTATIC))
        end
      end

      class Negate

      end

      class PushBoolean
        def initialize(value)
          @value = value
        end

        def emit_jvm_bytecode(methodgen, factory)
          list = methodgen.getInstructionList

          list.append(BCEL::ALOAD.new(RUNTIME_INDEX))
          if @value
            methodname = "getTrue"
          else
            methodname = "getFalse"
          end
          list.append(factory.createInvoke("org.jruby.Ruby",
                                           methodname,
                                           BCEL::ObjectType.new("org.jruby.RubyBoolean"),
                                           BCEL::Type[].new(0),
                                           BCEL::Constants::INVOKEVIRTUAL))
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
