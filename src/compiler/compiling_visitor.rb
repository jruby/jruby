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

require 'java'
require 'bytecode.rb'

module JavaLang
  include_package 'java.lang'
  java_alias :JString, :String
end

module JRuby
  module AST
    include_package 'org.jruby.ast.visitor'
    include_package 'org.jruby.ast'
  end
end

module JRuby
  module Compiler

    require 'bcel.rb'

    class JvmGenerator
      attr_reader :method
      attr_reader :factory

      def initialize(method)
        @method = method
        @factory =
          BCEL::InstructionFactory.new(method.getConstantPool)
      end

      def java_class_name
        @method.getClassName
      end

      def append(instruction)
        if BCEL::BranchInstruction.java_class.assignable_from?(instruction.java_class)
          # Javasupport problem workaround
          list = getInstructionList
          JavaClass.for_name("org.apache.bcel.generic.InstructionList").java_method(:append, "org.apache.bcel.generic.BranchInstruction").invoke(list.java_object, instruction.java_object)
        else
          getInstructionList.append(instruction)
        end
      end

      def appendInvoke(*invoke_arguments)
        append(factory.createInvoke(*invoke_arguments))
      end

      def appendPush(value)
        append(BCEL::PUSH.new(getConstantPool, value))
      end

      def getInstructionList
        method.getInstructionList
      end

      def getConstantPool
        method.getConstantPool
      end

      def getLocalVariables
        method.getLocalVariables
      end

      def addLocalVariable(name, type, start, stop)
        method.addLocalVariable(name, type, start, stop)
      end

      def getEnd
        getInstructionList.getEnd
      end
    end

    class BytecodeSequence
      include Enumerable

      def initialize
        @bytecodes = []
        @labels = []
        @methods = {}
        @method_arg_count = {}
      end

      def <<(bytecode)
        @bytecodes << bytecode
      end

      def each
        @bytecodes.each {|b| yield(b) }
      end

      def new_method(name, argument_count, &block)
        old_bytecodes = @bytecodes
        begin
          @bytecodes = []
          @methods[name] = @bytecodes
          @method_arg_count[name] = argument_count
          block.call
        ensure
          @bytecodes = old_bytecodes
        end
      end

      def [](index)
        @bytecodes[index]
      end

      def jvm_compile(classgen, name)
        methodgen = create_java_method(classgen, name, 0)
        generator = JvmGenerator.new(methodgen)
        @bytecodes.each {|b|
          b.emit_jvm_bytecode(generator)
        }
        methodgen.getInstructionList.append(BCEL::ARETURN.new)
        methodgen.setMaxStack
        classgen.addMethod(methodgen.getMethod)

        @methods.each {|name, bytecodes|
          methodgen = create_java_method(classgen,
                                         name,
                                         @method_arg_count[name])
          generator = JvmGenerator.new(methodgen)
          bytecodes.each {|b|
            b.emit_jvm_bytecode(generator)
          }
          methodgen.getInstructionList.append(BCEL::ARETURN.new)
          methodgen.setMaxStack
          classgen.addMethod(methodgen.getMethod)
        }
      end

      def create_java_method(classgen, name, argument_count)
        arg_types = BCEL::Type[].new(2 + argument_count)
        arg_types[0] = BCEL::ObjectType.new("org.jruby.Ruby")
        arg_types[1] = BCEL::ObjectType.new("org.jruby.runtime.builtin.IRubyObject")
        index = 2
        argument_count.times {
          arg_types[index] = BCEL::ObjectType.new("org.jruby.runtime.builtin.IRubyObject")
          index += 1
        }

        arg_names = JavaLang::JString[].new(2 + argument_count)
        arg_names[0] = "runtime"
        arg_names[1] = "self"
        index = 2
        argument_count.times {
          arg_names[index] = "arg_" + (index - 2).to_s
          index += 1
        }

        methodgen = BCEL::MethodGen.new(BCEL::Constants::ACC_PUBLIC | BCEL::Constants::ACC_STATIC,
                                        BCEL::ObjectType.new("org.jruby.runtime.builtin.IRubyObject"),
                                        arg_types,
                                        arg_names,
                                        name,
                                        classgen.getClassName,
                                        BCEL::InstructionList.new,
                                        classgen.getConstantPool)
      end
    end

    module CompilingVisitor
      include JRuby::Compiler::Bytecode

      def method_missing(name)
        raise "Missing implementation for #{name}"
      end

      def compile(tree)
        @bytecodes = BytecodeSequence.new(@constantpoolgen)
        emit_bytecodes(tree)
        @bytecodes
      end

      def emit_bytecodes(node)
        node.accept(self)
      end

      def visitNewlineNode(node)
        emit_bytecodes(node.getNextNode)
      end

      def visitLocalAsgnNode(node)
        emit_bytecodes(node.getValueNode)
        @bytecodes << AssignLocal.new(node.getCount)
      end

      def visitFixnumNode(node)
        @bytecodes << PushFixnum.new(node.getValue)
      end

      def visitCallNode(node)
        emit_bytecodes(node.getReceiverNode)
        iter = node.getArgsNode.iterator
        while iter.hasNext
          emit_bytecodes(iter.next)
        end
        @bytecodes << Call.new(node.getName,
                               node.getArgsNode.size,
                               :normal)
      end

      def visitFCallNode(node)
        @bytecodes << PushSelf.new
        unless node.getArgsNode.nil?
          iter = node.getArgsNode.iterator
          while iter.hasNext
            emit_bytecodes(iter.next)
          end
          arity = node.getArgsNode.size
        else
          arity = 0
        end
        @bytecodes << Call.new(node.getName,
                               arity,
                               :functional)
      end

      def visitStrNode(node)
        @bytecodes << PushString.new(node.getValue)
      end

      def visitSelfNode(node)
        @bytecodes << PushSelf.new
      end

      def visitNotNode(node)
        emit_bytecodes(node.getConditionNode)
        @bytecodes << Negate.new
      end

      def visitIfNode(node)
        emit_bytecodes(node.getCondition)
        iffalse = IfFalse.new(nil)
        @bytecodes << iffalse
        emit_bytecodes(node.getThenBody)
        goto_end = Goto.new(nil)
        @bytecodes << goto_end
        label_else = Label.new
        @bytecodes << label_else
        iffalse.target = label_else
        unless node.getElseBody.nil?
          emit_bytecodes(node.getElseBody)
        else
          @bytecodes << PushNil.new
        end
        label_end = Label.new
        @bytecodes << label_end
        goto_end.target = label_end
      end

      def visitFalseNode(node)
        @bytecodes << PushBoolean.new(false)
      end

      def visitTrueNode(node)
        @bytecodes << PushBoolean.new(true)
      end

      def visitArrayNode(node)
        @bytecodes << PushArray.new(node.size)
        iter = node.iterator
        while iter.hasNext
          emit_bytecodes(iter.next)
          @bytecodes << Call.new('<<', 1, :normal)
        end
      end

      def visitBlockNode(node)
        iter = node.iterator
        while iter.hasNext
          emit_bytecodes(iter.next)
        end
      end

      def visitDefnNode(node)
        @bytecodes.new_method(node.getName, node.getArgsNode.getArgsCount) {
          emit_bytecodes(node.getBodyNode)
        }
        @bytecodes << CreateMethod.new(node.getName,
                                       node.getArgsNode.getArgsCount)
        @bytecodes << PushNil.new
      end

      def visitScopeNode(node)
        @bytecodes << NewScope.new(node.getLocalNames)
        emit_bytecodes(node.getBodyNode)

        # ... finally
        @bytecodes << RestoreScope.new
      end

      def visitLocalVarNode(node)
        @bytecodes << PushLocal.new(node.getCount)
      end

      def visitBeginNode(node)
        emit_bytecodes(node.getBodyNode)
      end

      def visitDotNode(node)
        emit_bytecodes(node.getBeginNode)
        emit_bytecodes(node.getEndNode)
        @bytecodes << CreateRange.new(node.isExclusive)
      end

      def visitSymbolNode(node)
        @bytecodes << PushSymbol.new(node.getName)
      end

      def visitConstNode(node)
        @bytecodes << PushConstant.new(node.getName)
      end

      def visitNilNode(node)
        @bytecodes << PushNil.new
      end

      def visitModuleNode(node)
        # @bytecodes << GetModule.new(node.getName)
        # @bytecodes << AssignSelf.new

        # .... etc. ... 

        emit_bytecodes(node.getBodyNode)

        # .. finally

        # @bytecodes << RestoreSelf.new
        @bytecodes << PushNil.new
      end

      def visitColon2Node(node)
        emit_bytecodes(node.getLeftNode)
        # ... if module on stack
        #        getConstant(node.getName
        # ... else
        #        callMethod(node.getName)  .. no args
      end

      def visitConstDeclNode(node)
        emit_bytecodes(node.getValueNode)
        @bytecodes << AssignConstant.new(node.getName)
      end
    end

    # Since we can't subclass Java interfaces properly we have
    # to do this magic to get CompilingVisitor to behave like
    # a class.
    def CompilingVisitor.new()
      nodeVisitor = JRuby::AST::NodeVisitor.new
      class << nodeVisitor
        include CompilingVisitor
      end
      nodeVisitor
    end
  end
end
