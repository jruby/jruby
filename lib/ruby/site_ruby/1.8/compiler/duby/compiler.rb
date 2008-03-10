require 'compiler/builder'
require 'jruby'

module Compiler
  module PrimitiveRuby
    JObject = java.lang.Object
    JClass = java.lang.Class
    JString = java.lang.String
    Void = java.lang.Void::TYPE
    System = java.lang.System
    PrintStream = java.io.PrintStream
    JInteger = java.lang.Integer
    
    class CompileError < Exception
      def initialize(position, message)
        full_message = "Compile error at #{position.file}:#{position.start_line}: #{message}"
        super(full_message)
      end
    end
    
    # reload 
    module Java::OrgJrubyAst
      class Node
        def compile(builder)
          # default behavior is to raise, to expose missing nodes
          raise CompileError.new(position, "Unsupported syntax: #{self}")
        end
      end
  
      class ArgsNode
        def compile(builder)
          raise("PRuby only supports normal args") if opt_args || rest_arg != -1 || block_arg_node
          return unless args
          args.child_nodes.each do |arg|
            builder.local(arg.name)
          end
        end
      end
  
      class ArrayNode
        def compile(builder)
          # not implemented
          raise
        end
      end
      
      class ClassNode
        def compile(builder)
          cb = builder.public_class(cpath.name)
          body_node.compile(cb)
        end
      end
  
      class BlockNode
        def compile(builder)
          child_nodes.each do |node|
            builder.line node.position.start_line
            
            node.compile(builder)
          end
        end
        
        # Type of a block is the type of its final element
        def type(builder)
          child_nodes.get(child_nodes.size - 1).type
        end
      end
      
      class CallNode
        def compile(builder)
          receiver_type = receiver_node.type(builder)
          
          if Java::JavaClass === receiver_type && receiver_type.primitive?
            # we're performing an operation against a primitive, map it accordingly
            compile_primitive(receiver_type, builder)
          else
            receiver_node.compile(builder)

            # inefficient to cast every time; better inference will help
            builder.checkcast(receiver_type)

            args_list = args_node.child_nodes.to_a
            args_list.each_index do |idx|
              node = args_list[idx]
              node.compile(builder)
            end

            builder.invokevirtual receiver_type, mapped_name(builder), signature(builder)
          end
        end
        
        def compile_primitive(type, builder)
          receiver_node.compile(builder)

          if !args_node || args_node.size != 1
            raise CompileError.new(position, "Primitive operations must have exactly one argument")
          end
          
          node = args_node.get(0)
          # TODO: check or cast types according to receiver's type
          node.compile(builder)

          case type
          when JInteger::TYPE
            case name
            when "+"
              builder.iadd
            when "-"
              builder.isub
            else
              raise CompileError.new(position, "Primitive int operation #{name} not supported")
            end
          else
            raise CompileError.new(position, "Primitive #{type} operations not supported")
          end
        end
        
        def mapped_name(builder)
          # TODO move to a utility somewhere for smart name mappings
          mapped_name = name
          if (receiver_node.type(builder) == JString && name == "+")
            mapped_name = "concat"
          end
          
          mapped_name
        end
        
        def type(builder)
          @return_type ||= begin
            recv_type = receiver_node.type(builder)
            
            if Java::JavaClass === recv_type
              recv_type
            else
              recv_java_class = recv_type.java_class
              arg_types = []
              args_node.child_nodes.each do |node|
                arg_types << node.type(builder)
              end if args_node
              declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
              return_type = declared_method.return_type

              JavaUtilities.get_proxy_class(return_type.to_s)
            end
          end
        end
        
        def signature(builder)
          arg_types = []
          args_node.child_nodes.each do |node|
            arg_types << node.type(builder)
          end if args_node
          recv_java_class = receiver_node.type(builder).java_class
          declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
          return_type = declared_method.return_type
          if (return_type)
            return_class = JavaUtilities.get_proxy_class(return_type.to_s)
          else
            return_type = Void
          end
          
          return [
            return_class,
            *declared_method.parameter_types.map {|type| JavaUtilities.get_proxy_class(type.to_s)}
          ]
        end
        
        def declared_type(builder)
          elements = [name]
          receiver = receiver_node
          # walk receivers until we get to a vcall, the top of the declaration
          until VCallNode === receiver
            elements.unshift(receiver.name)
            receiver = receiver.receiver_node
          end
          # push VCall's name as first element
          elements.unshift(receiver.name)
          
          # join and load
          class_name = elements.join(".")
          JavaUtilities.get_proxy_class(class_name)
        end
      end
  
      class Colon2Node
        def declared_type(builder)
          left_node.declared_type(builder).java_class.declared_field(name).static_value
        end
      end
      
      class DefnNode
        def compile(builder)
          first_real_node = body_node
          first_real_node = body_node.child_nodes[0] if BlockNode === body_node
          while NewlineNode === first_real_node
            first_real_node = first_real_node.next_node
          end
          
          # determine signature from declaration line
          signature = first_real_node.signature(builder) if HashNode === first_real_node
          
          signature ||= [Void]
          
          builder.method2(name, *signature) do |method|
            # Run through any type declarations first
            first_real_node.declare_types(method) if HashNode === first_real_node

            # declare args that may not have been declared already
            args_node.compile(method)
            
            body_node.compile(method)
            
            # Expectation is that last element leaves the right type on stack
            case signature[0]
            when Void
              method.returnvoid
            when JInteger::TYPE
              method.ireturn
            else
              method.areturn
            end
          end
        end
      end
  
      class FCallNode
        def compile(builder)
          if (mapped_name(builder) == "println")
            builder.getstatic System, "out", [PrintStream]
            
            arg_types = []
            args_node.child_nodes.each do |node|
              node.compile(builder)
              arg_types << node.type(builder)
            end
            
            builder.invokevirtual PrintStream, mapped_name(builder), special_signature(PrintStream, builder)
          else
            builder.aload 0
            arg_types = []
            args_node.child_nodes.each do |node|
              node.compile(builder)
              arg_types << node.type(builder)
            end
            
            builder.invokevirtual builder.this, name, builder.method_signature(name, arg_types)
          end
        end
        
        def mapped_name(builder)
          if name == "puts"
            "println"
          end
        end
        
        def special_signature(recv_type, builder)
          arg_types = []
          args_node.child_nodes.each do |node|
            arg_types << node.type(builder)
          end if args_node
          recv_java_class = recv_type.java_class
          declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
          return_type = declared_method.return_type
          if (return_type)
            return_class = JavaUtilities.get_proxy_class(return_type.to_s)
          else
            return_class = Void
          end
          
          return [
            return_class,
            *declared_method.parameter_types.map {|type| JavaUtilities.get_proxy_class(type.to_s)}
          ]
        end
        
        def type(builder)
          arg_types = []
          args_node.child_nodes.each do |node|
            arg_types << node.type(builder)
          end if args_node
          builder.method_signature(name, arg_types)[0]
        end
      end
      
      class FixnumNode
        def compile(builder)
          builder.ldc_int(value)
        end
        
        def type(builder)
          JInteger::TYPE
        end
      end
      
      class HashNode
        def compile(builder)
          @declared ||= false
          unless @declared
            # TODO: compile
            super
          end
        end
        
        def declare_types(builder)
          @declared = true
          list = list_node.child_nodes.to_a
          list.each_index do |index|
            builder.local(list[index].name, list[index + 1].declared_type(builder)) if index % 2 == 0
          end
        end
        
        def signature(builder)
          @declared = true
          arg_types = []
          return_type = Void
          list = list_node.child_nodes.to_a
          list.each_index do |index|
            if index % 2 == 0
              if SymbolNode === list[index] && list[index].name == 'return'
                return_type = list[index + 1].declared_type(builder)
              else
                arg_types << list[index + 1].declared_type(builder)
              end
            end
          end
          return [return_type, *arg_types]
        end
      end
      
      class IfNode
        def compile(builder)
          f = builder.label
          done = builder.label
          condition = self.condition
          condition = condition.next_node while NewlineNode === condition
          
          case condition
          when CallNode
            case condition.receiver_node.type(builder)
            when JInteger::TYPE
              case condition.name
              when "<"
                args = condition.args_node
                raise CompileError.new(position, "int < must have exactly one argument") if !args || args.size != 1
                
                condition.receiver_node.compile(builder)
                args.get(0).compile(builder)
                
                # test >= for jump
                builder.if_icmpge(f)
                
                then_body.compile(builder)
                builder.goto(done)
                
                f.set!
                else_body.compile(builder)
                
                done.set!
              end
            else
              raise CompileError.new(position, "Conditionals on non-primitives not supported: #{condition.inspect}")
            end
          else
            raise CompileError.new(position, "Non-call conditionals not supported: #{condition.inspect}")
          end
        end
      end
      
      class LocalAsgnNode
        def compile(builder)
          local_index = builder.local(name, value_node.type(builder))
          value_node.compile(builder)
          case type(builder)
          when JInteger::TYPE
            builder.istore(local_index)
          else
            builder.astore(local_index)
          end
        end
        
        def type(builder)
          builder.local_type(name)
        end
      end
      
      class LocalVarNode
        def compile(builder)
          local_index = builder.local(name)
          case type(builder)
          when JInteger::TYPE
            builder.iload(local_index)
          else
            builder.aload(local_index)
          end
        end
        
        def type(builder)
          builder.local_type(name)
        end
      end
  
      class NewlineNode
        def compile(builder)
          builder.line position.start_line
          next_node.compile(builder)
        end
      end
      
      class ReturnNode
        def compile(builder)
          value_node.compile(builder)
          builder.areturn
        end
      end
  
      class RootNode
        def compile(builder)
          # builder is class builder
      
          if body_node
            body_node.compile(builder)
          end
        end
      end
      
      class SymbolNode
        
      end
  
      class StrNode
        def compile(builder)
          builder.ldc value
        end
        
        def type(builder)
          java.lang.String
        end
      end
  
      class VCallNode
        def compile(builder)
          builder.aload 0

          builder.invokevirtual builder.this, name, builder.method_signature(name, [])
        end
        
        def mapped_name(builder)
          # TODO map names for the local type?
          name
        end
        
        def type(builder)
          builder.method_signature(name, [])[0]
        end
      end
    end
  end
end

if $0 == __FILE__
  n = JRuby.parse(File.read(ARGV[0]), ARGV[0])
  compiler = Compiler::FileBuilder.new(ARGV[0])
  n.compile(compiler)
  
  compiler.generate
end