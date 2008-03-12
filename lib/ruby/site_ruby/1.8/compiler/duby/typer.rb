require 'compiler/builder'
require 'jruby'

module Compiler
  module PrimitiveRuby
    # reload 
    module Java::OrgJrubyAst
      class Node
      end
  
      class ArgsNode
      end
  
      class ArrayNode
      end
      
      class ClassNode
      end
  
      class BlockNode
        # Type of a block is the type of its final element
        def type(builder)
          child_nodes.get(child_nodes.size - 1).type
        end
      end
      
      class CallNode
        def mapped_name(builder)
          # TODO move to a utility somewhere for smart name mappings
          # TODO or at least make it a table...
          mapped_name = name
          case receiver_node.type(builder)
          when JString
            case name
            when "+"
              mapped_name = "concat"
            # This doesn't work yet, because format is a static method on String
#            when "%"
#              mapped_name = "format"
            end
          else
            case name
            when "new"
              mapped_name = "<init>"
            end
          end
          
          mapped_name
        end
        
        def type(builder)
          @return_type ||= begin
            recv_type = receiver_node.type(builder)
            
            # if we already have an exact class, use it
            if recv_type.array?
              recv_type = recv_type.component_type
            elsif JavaClass === recv_type
              recv_type
            else
              # otherwise, find the target method and get its return type
              recv_java_class = recv_type
              arg_types = []
              args_node.child_nodes.each do |node|
                arg_types << node.type(builder)
              end if args_node
              declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
              return_type = declared_method.return_type

              builder.type(return_type.to_s)
            end
          end
        end
        
        def signature(builder)
          arg_types = []
          args_node.child_nodes.each do |node|
            arg_types << node.type(builder)
          end if args_node
          
          recv_java_class = receiver_node.type(builder)
          declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
          return_type = declared_method.return_type
          
          if (return_type)
            return_class = builder.type(return_type.to_s)
          else
            return_type = Void
          end
          
          return [
            return_class,
            *declared_method.parameter_types.map {|type| builder.type(type.to_s)}
          ]
        end
        
        def declared_type(builder)
          if name == "[]"
            # array type, top should be a constant; find the rest
            array = true
            elements = []
          else
            elements = [name]
          end
          
          receiver = receiver_node
          
          loop do
            case receiver
            when ConstNode
              elements << receiver_node.name
              break
            when CallNode
              elements.unshift(receiver.name)
              receiver = receiver.receiver_node
            when VCallNode
              elements.unshift(receiver.name)
              break
            end
          end
          
          # join and load
          class_name = elements.join(".")
          type = builder.type(class_name)
          
          if array
            type.array_class
          else
            type
          end
        end
      end
  
      class Colon2Node
        def declared_type(builder)
          left_node.declared_type(builder).declared_field(name).static_value
        end
      end
      
      class ConstNode
        def type(builder)
          builder.type(name)
        end
      end
      
      
      class DefnNode
        def mapped_name(builder)
          case name
          when "initialize"
            "<init>"
          else
            name
          end
        end
      end
      
      class DefsNode
      end
  
      class FCallNode
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
          recv_java_class = recv_type
          declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
          return_type = declared_method.return_type
          if (return_type)
            return_class = return_type
          else
            return_class = Void
          end
          
          return [
            return_class,
            *declared_method.parameter_types
          ]
        end
        
        def type(builder)
          arg_types = []
          args_node.child_nodes.each do |node|
            arg_types << node.type(builder)
          end if args_node
          if builder.static
            signature = builder.static_signature(name, arg_types)
          else
            signature = builder.instance_signature(name, arg_types)
          end
          raise CompileError.new(position, "Signature not found for call #{name}") unless signature
          
          signature[0]
        end
      end
      
      class FixnumNode
        def type(builder)
          Jint
        end
      end
      
      class HashNode
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
      end
      
      class InstVarNode
        def type(builder)
          builder.field_type(mapped_name(builder))
        end
        
        def mapped_name(builder)
          # TODO: strip off the @ sigil?
          name
        end
      end
      
      class InstAsgnNode
        def type(builder)
          builder.field(mapped_name(builder), value_node.type(builder))
          builder.field_type(mapped_name(builder))
        end
        
        def mapped_name(builder)
          # TODO: strip off the @ sigil?
          name
        end
      end
      
      class LocalAsgnNode
        def type(builder)
          builder.local_type(name)
        end
      end
      
      class LocalVarNode
        def type(builder)
          builder.local_type(name)
        end
      end
      
      class ModuleNode
      end
  
      class NewlineNode
        def type(builder)
          next_node.type(builder)
        end
      end
      
      class ReturnNode
      end
  
      class RootNode
      end
      
      class SelfNode
      end
  
      class StrNode
        def type(builder)
          java.lang.String
        end
      end
      
      class SymbolNode
        def declared_type(builder)
          builder.type(name.intern)
        end
      end
  
      class VCallNode
        def mapped_name(builder)
          # TODO map names for the local type?
          name
        end
        
        def type(builder)
          if builder.static
            builder.static_signature(name, [])[0]
          else
            builder.instance_signature(name, [])[0]
          end
        end
      end
    end
  end
end
