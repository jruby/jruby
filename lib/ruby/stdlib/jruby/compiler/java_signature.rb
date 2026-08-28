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
      visitor = JavaSignatureVisitor.new {|name| as_java_type(name)}
      annotations.inject({}) do |hash, anno_node|
        ((type, parsed)) = *anno_node.accept(visitor)
        hash[type] = parsed
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

    # @deprecated
    def primitive?(str); self.class.primitive_type(str) end

    class << self; private end

    def self.primitive_type(type)
      org.jruby.javasupport.JavaUtil.getPrimitiveClass(type) # null if not
    end

    def self.make_class_jiable(string)
      list = []
      first_cap = nil
      string.split('.').inject(false) do |last_cap, segment|
        if segment =~ /[A-Z]/
          list << ( last_cap ? "::#{segment}" : ".#{segment}" )
          first_cap = true if first_cap.nil?
          true # last_cap
        else
          list << ".#{segment}"
          first_cap = false if first_cap.nil?
          false # last_cap
        end
      end
      prefix = first_cap ? "" : "Java::" # if the first is capitalized, try java imports
      # e.g. [".java", ".lang", ".String"] or [".byte[]"]
      "#{prefix}#{list.join('')[1..-1]}"
    end

  end
  
  class JavaSignatureVisitor
    include org.jruby.ast.java_signature.AnnotationVisitor
    def initialize &blk
       @lookup = blk
       @target_type = []
       @antiwrap = nil # if we pass the number literals through the ruby/java interface, they are converted to Fixnums, and then Longs. Save them here to avoid conversion
    end
    
    def to_class(typename)
      @lookup.call typename
    end
    
    def annotation(anno)
        clazz = to_class(anno.name)
        java_clazz = clazz.java_class.to_java
        @antiwrap = { java_clazz =>
          anno.parameters.inject({}) do |hash, param|
          @target_type << java_clazz.get_method(param.name).return_type
          param.expression.accept(self)
          hash[param.name] = @antiwrap
          @target_type.pop
          hash
        end.to_java}.to_java
    end

    def annotation_array(aa)
      @antiwrap = aa.expressions.map do |expr|
        expr.accept self
        @antiwrap
      end.to_java java.lang.Object
    end

    def literal(lit)
      @antiwrap = lit.literal
    end

    def char_literal(lit)
      @antiwrap = lit.literal.to_java :char
    end
    
    def number_literal(lit)
      target = @target_type.last
      @antiwrap = if lit.float?
        lit.literal.to_f.to_java target
      else
        str = lit.literal
        base = str.start_with?("0x") ? 16 : 10
        signed = str.start_with?("-")
        str = str[2..-1] if base != 10 # strip off any base prefixes
        jll = java.lang.Long 
        longv = (signed ? jll.parseLong(str) : jll.parseUnsignedLong(str, base)).to_java :long
        if target == jll
           longv
        else
          longv.send("#{target.simple_name.downcase}Value").to_java target
        end
      end
    end

    def type(type)
      @antiwrap = to_class(type.wrapper_name).to_java
    end
  end
end
