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

      def initialize(method, factory)
        @method, @factory = method, factory
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
      end

      def <<(bytecode)
        @bytecodes << bytecode
      end

      def each
        @bytecodes.each {|b| yield(b) }
      end

      def [](index)
        @bytecodes[index]
      end

      def jvm_compile(methodgen)
        factory = BCEL::InstructionFactory.new(methodgen.getConstantPool)
        generator = JvmGenerator.new(methodgen, factory)
        @bytecodes.each {|b|
          b.emit_jvm_bytecode(generator)
        }
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
        iter = node.getArgsNode.iterator
        while iter.hasNext
          emit_bytecodes(iter.next)
        end
        @bytecodes << Call.new(node.getName,
                               node.getArgsNode.size,
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
        # ... node.getArgsNode ..?

        # ... change bytecode output to a new method

        emit_bytecodes(node.getBodyNode)

        # add the method to the system

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
