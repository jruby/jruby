require 'duby/typer'
require 'duby/jvm/method_lookup'
require 'java'

module Duby
  module Typer
    class JavaTyper < BaseTyper
      include Duby::JVM::MethodLookup
      
      def initialize
        type_mapper[AST::type(:string)] = AST::type("java.lang.String")
      end
      
      def name
        "Java"
      end

      def type_mapper
        @type_mapper ||= {}
      end

      def mapped_type(type)
        type_mapper[type] || type
      end
      
      def method_type(typer, target_type, name, parameter_types)
        case target_type
        when AST.type(:long)
          case name
          when '-'
            return nil if parameter_types.length != 1
            return nil if parameter_types[0] != AST.type(:long)
            return AST.type(:long)
          else
            log "Unknown method \"#{name}\" on type long"
          end
        else
          mapped_target = mapped_type(target_type)
          mapped_parameters = parameter_types.map {|type| mapped_type(type)}
          begin
            java_type = Java::JavaClass.for_name(mapped_target.name)
            arg_types = mapped_parameters.map {|type| Java::JavaClass.for_name(type.name)}
          rescue NameError
            Typer.log "Failed to infer Java types for method \"#{name}\" #{mapped_parameters} on #{mapped_target}"
            return nil
          end
          
          method = find_method(java_type, name, arg_types, mapped_target.meta?)

          if Java::JavaConstructor === method
            result = AST::type(method.declaring_class.name)
          else
            result = AST::type(method.return_type.name)
          end

          if result
            log "Method type for \"#{name}\" #{parameter_types} on #{target_type} = #{result}"
          else
            log "Method type for \"#{name}\" #{parameter_types} on #{target_type} not found"
          end

          result
        end
      end
    end
  end

  typer_plugins << Typer::JavaTyper.new
end