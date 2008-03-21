require 'compiler/builder'
require 'jruby'

module Compiler::Duby
  module AST
    class InferenceError < Exception
      attr_accessor :node
      def initialize(node, msg)
        super(msg)
        @node = node
      end
    end
    class Arguments
      def infer_type(typer)
        args.each {|arg| arg.infer_type(typer)}
      end
    end
    
    class RequiredArgument
      def infer_type(typer)
        unless @inferred_type
          # if not already typed, check parent of parent (MethodDefinition) for signature info
          method_def = parent.parent
          signature = method_def.signature

          # if signature, search for this argument
          if (signature.length == 1)
            @inferred_type = typer.learn_local_type(name, typer.default_type)
          else
            @inferred_type = typer.learn_local_type(name, signature[name.intern] || typer.default_type)
          end
        end
        
        @inferred_type
      end
    end
    
    class Body
      # Type of a block is the type of its final element
      def infer_type(typer)
        unless @inferred_type
          if children.size == 0
            @inferred_type = typer.default_type
          else
            children.each {|child| @inferred_type = child.infer_type(typer)}
          end
        end

        @inferred_type
      end
    end

    class Call
    end

    class FunctionalCall
    end

    class Constant
    end

    class Fixnum
      def infer_type(typer)
        typer.fixnum_type
      end
    end

    class Float
      def infer_type(typer)
        typer.float_type
      end
    end

    class Field
      def type(builder)
        @inferred_type ||= builder.field_type(mapped_name(builder))
      end
    end

    class FieldAssignment
      def type(builder)
        builder.field(mapped_name(builder), value_node.type(builder))
        @type ||= builder.field_type(mapped_name(builder))
      end
    end

    class LocalAssignment
      def infer_type(typer)
        @inferred_type ||= typer.learn_local_type(name, value.infer_type(typer))
      end
    end

    class Local
      def infer_type(typer)
        @inferred_type ||= typer.local_type(name.intern)
      end
    end

    class Module
    end

    class MethodDefinition
      def infer_type(typer)
        arguments.infer_type(typer)
        forced_type = signature[:return]
        inferred_type = body.infer_type(typer)
        
        if forced_type != TypeReference::NoType && !forced_type.is_parent(inferred_type)
          raise InferenceError.new(self, "Inferred return type is incompatible with declared")
        end
        
        @inferred_type = typer.learn_method_type(name, inferred_type)
      end
    end

    class StaticMethodDefinition
      def infer_type(typer)
        arguments.infer_type(typer)
        forced_type = signature[:return]
        inferred_type = body.infer_type(typer)
        
        if forced_type != TypeReference::NoType && !forced_type.is_parent(inferred_type)
          raise InferenceError.new(self, "Inferred return type is incompatible with declared")
        end
        
        @inferred_type = typer.learn_method_type(name, inferred_type)
      end
    end

    class Noop
      def infer_type(typer)
        @inferred_type ||= typer.default_type
      end
    end

    class String
      def infer_type(typer)
        @inferred_type ||= typer.string_type
      end
    end
  end
end
