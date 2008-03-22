require 'compiler/duby/ast'
require 'compiler/duby/transform'

module Compiler::Duby
  module Typer
    class InferenceError < Exception
      attr_accessor :node
      def initialize(msg, node = nil)
        super(msg)
        @node = node
      end
    end
    
    class Simple
      include Compiler::Duby
      attr_accessor :self_type

      def initialize(self_type)
        @self_type = type_reference(self_type)
      end

      def default_type
        nil
      end
      def fixnum_type
        AST::TypeReference.new :fixnum
      end
      def float_type
        AST::TypeReference.new :float
      end
      def string_type
        AST::TypeReference.new :string
      end
      def learn_local_type(scope, name, type)
        name = name.intern unless Symbol === name

        puts "* Learned local type in #{scope} : #{name} = #{type}" if $DEBUG

        get_local_type_hash(scope)[name] = type
      end
      def local_type(scope, name)
        name = name.intern unless Symbol === name

        puts "* Retrieved local type in #{scope} : #{name} = #{get_local_type_hash(scope)[name]}" if $DEBUG

        get_local_type_hash(scope)[name]
      end
      def local_types
        @local_types ||= {}
      end
      def get_local_type_hash(scope)
        local_types[scope] ||= {}
      end
      def learn_method_type(target_type, name, parameter_types, type)
        name = name.intern unless Symbol === name

        puts "* Learned method: #{target_type} : #{name} : (#{parameter_types.join(',')}) = #{type}" if $DEBUG

        get_method_type_hash(target_type, name, parameter_types)[:type] = type
      end
      def method_type(target_type, name, parameter_types)
        name = name.intern unless Symbol === name

        get_method_type_hash(target_type, name, parameter_types)[:type]
      end
      def method_types
        @method_types ||= {}
      end
      def get_method_type_hash(target_type, name, parameter_types)
        method_types[target_type] ||= {}
        method_types[target_type][name] ||= {}
        method_types[target_type][name][parameter_types.size] ||= {}
        
        current = method_types[target_type][name][parameter_types.size]

        parameter_types.each {|type| current[type] ||= {}; current = current[type]}

        current
      end
      def type_reference(name)
        AST::TypeReference.new(name)
      end

      def deferred_nodes
        @deferred_nodes ||= []
      end

      def defer_inference(node)
        return if deferred_nodes.include? node
        puts "* Deferring inference for #{node}" if $DEBUG
        deferred_nodes << node
      end

      def resolve_deferred(raise = false)
        count = deferred_nodes.size + 1
        count.times do |i|
          old_deferred = @deferred_nodes
          @deferred_nodes = deferred_nodes.select do |node|
            type = node.infer_type(self)

            puts "* [Cycle #{i}]: Inferred type for #{node}: #{type || 'FAILED'}" if $DEBUG

            type == default_type
          end
          
          if @deferred_nodes.size == 0
            puts "* Inference cycle #{i} resolved all types, exiting" if $DEBUG
            break
          elsif old_deferred == @deferred_nodes
            puts "* Inference cycle #{i} made no progress, bailing out" if $DEBUG
            break
          end
        end

        # done with n sweeps, if any remain raise errors
        if raise && !deferred_nodes.empty?
          raise InferenceError.new("Could not infer typing for the following nodes:\n  " + deferred_nodes.map {|e| "#{e} (child of #{e.parent})"}.join("\n  "))
        end
      end
    end
  end
  InferenceError = Typer::InferenceError
  SimpleTyper = Typer::Simple
  
  module AST
    class Arguments
      def infer_type(typer)
        unless @inferred_type
          @inferred_type = args ? args.map {|arg| arg.infer_type(typer)} : []
        end
      end
    end
    
    class RequiredArgument
      def infer_type(typer)
        unless @inferred_type
          # if not already typed, check parent of parent (MethodDefinition) for signature info
          method_def = parent.parent
          signature = method_def.signature

          # if signature, search for this argument
          if signature[name.intern]
            @inferred_type = typer.learn_local_type(scope, name, signature[name.intern])
          else
            @inferred_type = typer.local_type(scope, name)
          end
          
          unless @inferred_type
            typer.defer_inference(self)
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
          
          unless @inferred_type
            typer.defer_inference(self)
          end
        end

        @inferred_type
      end
    end

    class Call
      def infer_type(typer)
        unless @inferred_type
          receiver_type = target.infer_type(typer)
          parameter_types = parameters.map {|param| param.infer_type(typer)}
          @inferred_type = typer.method_type(receiver_type, name, parameter_types)
          
          unless @inferred_type
            typer.defer_inference(self)
          end
        end
        
        @inferred_type
      end
    end

    class FunctionalCall
      def infer_type(typer)
        unless @inferred_type
          receiver_type = typer.self_type
          parameter_types = parameters.map {|param| param.infer_type(typer)}
          @inferred_type = typer.method_type(receiver_type, name, parameter_types)
          
          unless @inferred_type
            typer.defer_inference(self)
          end
        end
        
        @inferred_type
      end
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
        unless @inferred_type
          @inferred_type = typer.learn_local_type(scope, name, value.infer_type(typer))
          
          unless @inferred_type
            typer.defer_inference(self)
          end
        end
        
        @inferred_type
      end
    end

    class Local
      def infer_type(typer)
        unless @inferred_type
          @inferred_type = typer.local_type(scope, name)
          
          unless @inferred_type
            typer.defer_inference(self)
          end
        end
        
        @inferred_type
      end
    end

    class Module
    end

    class MethodDefinition
      def infer_type(typer)
        arguments.infer_type(typer)
        forced_type = signature[:return]
        inferred_type = body.infer_type(typer)
        
        if !inferred_type
          typer.defer_inference(self)
        else
          if forced_type != TypeReference::NoType && !forced_type.is_parent(inferred_type)
            raise InferenceError.new("Inferred return type is incompatible with declared", self)
          end

          @inferred_type = typer.learn_method_type(typer.self_type, name, arguments.inferred_type, inferred_type)
          signature[:return] = @inferred_type
        end
        
        @inferred_type
      end
    end

    class StaticMethodDefinition
      def infer_type(typer)
        arguments.infer_type(typer)
        forced_type = signature[:return]
        inferred_type = body.infer_type(typer)
        
        if !inferred_type
          typer.defer_inference(self)
        else
          if forced_type != TypeReference::NoType && !forced_type.is_parent(inferred_type)
            raise InferenceError.new("Inferred return type is incompatible with declared", self)
          end

          # TODO: this doesn't separate static from instance
          @inferred_type = typer.learn_method_type(typer.self_type, name, arguments.inferred_type, inferred_type)
        end
        
        @inferred_type
      end
    end

    class Noop
      def infer_type(typer)
        @inferred_type ||= typer.default_type
      end
    end
    
    class Script
      def infer_type(typer)
        @inferred_type ||= body.infer_type(typer) || (typer.defer_inference(self); nil)
      end
    end

    class String
      def infer_type(typer)
        @inferred_type ||= typer.string_type
      end
    end
  end
end

if __FILE__ == $0
  ast = Compiler::Duby::AST.parse(File.read(ARGV[0]))
  typer = Compiler::Duby::SimpleTyper.new(:script)
  ast.infer(typer)
  typer.resolve_deferred(true)
  
  p ast
end