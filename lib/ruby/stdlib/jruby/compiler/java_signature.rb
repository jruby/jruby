require 'java'

module JRuby
  class JavaSignature

    def self.parse(signature)
      string = signature.to_s
      stream = java.io.ByteArrayInputStream.new(string.to_java_bytes)
      new string, org.jruby.parser.JavaSignatureParser.parse(stream)
    end

    def initialize(string, ast)
      @string, @ast = string, ast
    end

    def name
      @ast.name.to_s
    end

    def as_java_type(string); self.class.as_java_type(string)  end

    # FIXME: Can make this accept whole list too if that is actual contract
    # FIXME: Can be literals too
    def self.as_java_type(type)
      type = type.to_s # toString for org.jruby.ast.java_signature.TypeNode
      prim = primitive_type(type); return prim if prim

      # If annotation makes it in strip @ before we try and match it.
      type = type[1..-1] if type.start_with? '@'

      eval make_class_jiable(type)
    end

    ##
    # return live JI proxy for return type
    def return_type
      as_java_type(@ast.return_type)
    end

    ##
    # {org.bukkit.event.EventHandler => {}, }
    def annotations
      annotations = @ast.modifiers.select(&:is_annotation?)
      annotations.inject({}) do |hash, anno_node|
        hash[as_java_type(anno_node.name)] = process_annotation_params(anno_node)
        hash
      end
    end

    def modifiers
      @ast.modifiers.reject(&:is_annotation?)
    end

    ##
    # return JI proxies for all parameters (excluding return_type)
    def parameters
      @ast.parameters.map { |p| as_java_type(p.type) }
    end

    ##
    # {return_type, *parameters} tuple (JI proxies)
    def types
      [return_type, *parameters]
    end

    def to_s
      @string
    end

    def to_s_ast
      @ast
    end

    def inspect
      <<-EOS
name       : #{name}
modifiers  : #{modifiers.join(', ')}
annotations: #{annotations.map{|k, v| "#{k}(#{v.inspect})"}.join(', ')}
parameters : #{parameters.join(', ')}
return_type: #{return_type}
    EOS
    end

    private

    def process_annotation_params(anno_node)
      anno_node.parameters.inject({}) do |hash, param|
        # ??? default will be nil/null name here.
      hash[param.name] = param.expression.accept(JavaSignatureVisitor.new{|name| as_java_type(name)})
        hash
      end
    end

    # @deprecated
    def primitive?(str); self.class.primitive_type(str) end

    class << self; private end

    def self.primitive_type(type)
      org.jruby.javasupport.JavaUtil.getPrimitiveClass(type) # null if not
    end

    def self.make_class_jiable(string)
      list = []
      string.split('.').inject(false) do |last_cap, segment|
        if segment =~ /[A-Z]/
          list << ( last_cap ? "::#{segment}" : ".#{segment}" )
          true # last_cap
        else
          list << ".#{segment}"
          false # last_cap
        end
      end
      # e.g. [".java", ".lang", ".String"] or [".byte[]"]
      "Java::#{list.join('')[1..-1]}"
    end

  end
  
  class JavaSignatureVisitor
    include org.jruby.ast.java_signature.AnnotationVisitor
    def initialize &blk
       @lookup = blk
    end
    def annotation(anno)
        puts "annotation: #{anno.name}"
        { anno.name =>
        Hash[anno.parameters.map do |param|
          puts "   #{param.name} =>"
          [param.name, param.expression.accept(self)]
        end].to_java}.to_java
    end

    def annotation_array(aa)
      puts "annoa: #{aa}"
      aa.expressions.map do |expr|
        puts "  [] =>"
        expr.accept self
      end.to_java java.lang.Object
    end
    
    def literal(lit)
      puts "Literal: #{lit.literal}"
      lit.literal
    end

    def type(type)
      puts "type: #{type}"
      @lookup.call type
    end
  end
end
