
require 'rexml/sax2parser'


INCLUDES = %w(
org.jruby.Ruby
org.jruby.RubyModule
org.jruby.runtime.builtin.IRubyObject
org.jruby.runtime.builtin.definitions.MethodContext
org.jruby.runtime.builtin.definitions.ModuleDefinition
org.jruby.runtime.builtin.definitions.ModuleFunctionsContext
org.jruby.util.Asserts
)

class MethodDescription

  attr :arity, true
  attr :java_name, true
  attr :name

  def initialize(generator, name, count)
    @generator, @name, @count = generator, name, count
    @arity = 0
    @is_optional = false
    @java_name = name
  end

  def optional=(optional)
    @is_optional = optional
  end

  def optional?
    @is_optional
  end

  def generate_constant(output)
    output.write('public static final int ')
    output.write("#{constant_name} = ")
    output.write("#{@generator.constant_name} | #{@count.to_s};\n")
  end

  def generate_creation(output)
    output.write("context.")
    if @is_optional
      output.write("createOptional")
    else
      output.write("create")
    end
    output.write("(\"#{@name}\", #{constant_name}, #{arity});\n")
  end

  def constant_name
    @java_name.upcase
  end
end


class StaticMethodDescription < MethodDescription

  def generate_constant(output)
    output.write('public static final int ')
    output.write("#{constant_name} = ")
    output.write("STATIC | #{@count.to_s};\n")
  end

  def generate_switch_case(output)
    output.write("case #{constant_name} :\n")
    output.write("return #{@generator.implementation}.#{java_name}(")
    output.write("receiver")
    if optional?
      output.write(", args")
    else
      (0...arity).each {|i|
        output.write(", args[#{i}]")
      }
    end
    output.write(");\n")
  end
end


class Alias

  def initialize(name, original)
    @name, @original = name, original
  end

  def generate_constant(output)
    # no-op
  end

  def generate_creation(output)
    output.write('context.')
    if @original.optional?
      output.write("createOptional")
    else
      output.write("create")
    end
    output.write("(\"#{@name}\", #{constant_name}, #{arity});\n")
  end

  def constant_name
    @original.constant_name
  end

  def arity
    @original.arity
  end
end


class MethodGenerator

  def initialize(input)
    @input = input
    @is_module = false
    @name = nil
    @methods = []
    @class_methods = []
    @implementation = nil
  end

  attr :implementation

  def constant_name
    @name.upcase
  end

  def generate(output)
    read_input

    output.write("/* Generated - do not edit! */\n")
    output.write("\n")
    write_includes(output)
    output.write("\n")
    output.write("public class #{@name}Definition")
    output.write(" extends ClassDefinition {\n")
    output.write("private static final int #{constant_name} = 0xf000;\n")
    output.write("private static final int STATIC = #{constant_name} | 0x100;\n")
    @methods.each {|m|
      m.generate_constant(output)
    }
    @class_methods.each {|m|
      m.generate_constant(output)
    }
    output.write("\n")
    output.write("protected void defineMethods(MethodContext context) {\n")
    @methods.each {|m|
      m.generate_creation(output)
    }
    output.write("}\n")
    output.write("\n")
    output.write("protected void defineSingletonMethods(SingletonMethodContext context) {\n")
    @class_methods.each {|m|
      m.generate_creation(output)
    }
    output.write("}\n")

    output.write("public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {\n")
    output.write("switch (index) {\n")
    @class_methods.each {|m|
      m.generate_switch_case(output)
    }
    output.write("default :\n")
    output.write("Asserts.notReached();\n")
    output.write("return null;\n")
    output.write("}\n")
    output.write("}\n")

    output.write("}\n")
  end

  def write_includes(output)
    INCLUDES.each {|include|
      output.write("import #{include};\n")
    }
  end

  def read_input
    method_count = nil
    methods = nil
    method_description_class = nil

    parser = REXML::SAX2Parser.new(@input)
    parser.listen(:start_element, ["module"]) {|uri, localname, qname, attributes|
      if attributes['type'] == "module"
        @is_module = true
      end
    }
    parser.listen(:characters, ['^name$']) {|text|
      @name = text
    }
    parser.listen(:characters, ['^implementation$']) {|text|
      @implementation = text
    }
    parser.listen(:start_element, ['^instance\-methods$']) {
      methods = @methods
      method_count = 0
      method_description_class = MethodDescription
    }
    parser.listen(:start_element, ['^class\-methods$']) {
      methods = @class_methods
      method_count = 0
      method_description_class = StaticMethodDescription
    }
    parser.listen(:start_element, ['^method$']) {|uri, localname, qname, attributes|
      method_count += 1
      methods << method_description_class.new(self, attributes['name'], method_count)
    }
    parser.listen(:start_element, ['^arity$']) {|uri, localname, qname, attributes|
      if attributes.has_key?('optional')
        methods.last.optional = (attributes['optional'] == 'true')
      end
    }
    parser.listen(:characters, ['^arity$']) {|text|
      methods.last.arity = text.to_i
    }
    parser.listen(:characters, ['^java$']) {|text|
      methods.last.java_name = text
    }
    parser.listen(:start_element, ['^method\-alias$']) {|uri, localname, qname, attributes|
      original_name = attributes['original']
      original = methods.detect {|m| m.name == original_name }
      name = attributes['name']
      methods << Alias.new(name, original)
    }
    parser.parse
  end
end

if $0 == __FILE__
  generator = MethodGenerator.new(STDIN)
  generator.generate(STDOUT)
end
