require 'duby/typer'
require 'java'

module Duby
  module Typer
    class JavaTyper < BaseTyper
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
        java_type = Java::JavaClass.for_name(mapped_type(target_type).name)
        arg_types = parameter_types.map {|type| Java::JavaClass.for_name(mapped_type(type).name)}
        method = java_type.java_method(name, *arg_types)

        result = AST::type(method.return_type.name)
        
        if result
          log "Method type for \"#{name}\" #{parameter_types} on #{target_type} = #{result}"
        else
          log "Method type for \"#{name}\" #{parameter_types} on #{target_type} not found"
        end
        
        result
      end
    end
  end

  typer_plugins << Typer::JavaTyper.new
end