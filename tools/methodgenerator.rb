
require 'rexml/sax2parser'

INCLUDES = %w(
org.jruby.Ruby
org.jruby.RubyClass
org.jruby.RubyModule
org.jruby.runtime.builtin.IRubyObject
org.jruby.runtime.builtin.definitions.MethodContext
org.jruby.runtime.builtin.definitions.SingletonMethodContext
org.jruby.runtime.builtin.definitions.ModuleDefinition
org.jruby.runtime.builtin.definitions.ClassDefinition
org.jruby.runtime.builtin.definitions.ModuleFunctionsContext
org.jruby.util.Asserts
)


class AbstractMethodDescription

	def generate_constant(output)
		# no-op
	end

	def generate_creation(output)
		# no-op
	end

	def generate_switch_case(output)
		# no-op
	end
end


class MethodDescription < AbstractMethodDescription

	attr :arity, true
	attr :java_name, true
	attr :name

	def initialize(class_description, name, count)
		@class_description, @name, @count = class_description, name, count
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
		output << 'public static final int '
		output << "#{constant_name} = "
		output << "#{@class_description.constant_name} + #{@count.to_s};\n"
	end

	def generate_creation(output)
		output << "context."
		if @is_optional
			output << "createOptional"
		else
			output << "create"
		end
		output << "(\"#{@name}\", #{constant_name}, #{arity});\n"
	end

	def constant_name
		@java_name.upcase
	end
end


class StaticMethodDescription < MethodDescription

	def generate_constant(output)
		output << 'public static final int '
		output << "#{constant_name} = "
		output << "STATIC + #{@count.to_s};\n"
	end

	def generate_switch_case(output)
		output << "case #{constant_name} :\n"
		output << "return #{@class_description.implementation}.#{java_name}("
		output << "receiver"
		if optional?
			output << ", args"
		else
			(0...arity).each {|i|
			output << ", args[#{i}]"
		}
		end
		output << ");\n"
	end
end


class Alias < AbstractMethodDescription

	attr :name

	def initialize(name, original)
		@name, @original = name, original
	end

	def generate_creation(output)
		output << 'context.'
		if @original.optional?
			output << "createOptional"
		else
			output << "create"
		end
		output << "(\"#{@name}\", #{constant_name}, #{arity});\n"
	end

	def constant_name
		@original.constant_name
	end

	def arity
		@original.arity
	end
end


class UndefineMethod < AbstractMethodDescription

	def initialize(name)
		@name = name
	end

	def generate_creation(output)
		output << 'context.undefineMethod("'
		output << @name
		output << '");' + "\n"
	end
end


class ClassDescription

	attr_writer :is_module
	attr :name, true
	attr :methods, true
	attr :class_methods, true
	attr :implementation, true
	attr :package, true
	attr :included_modules, false
	def compute_start_index(generator)
		if (@superclass == "none" || @name == "Object")
			@start_index = 0
			return
		end
		superclass = generator.classes[@superclass]
		if superclass == nil
			open(@superclass + ".xml") { |input|
				superclass = generator.parseFile(input)
			}
			if superclass == nil
				@start_index = 0
				return
			end
			generator.generate_java(superclass)
		end
		@start_index = superclass.lastMethodIndex
	end
	def lastMethodIndex
		@start_index + @methods.size + 1
	end
	def initialize
		@is_module = false
		@name = nil
		@methods = []
		@class_methods = []
		@implementation = nil
		@superclass = "Object"
		@package = $package
		@included_modules = []
	end

	def superclass=(superclass)
		raise "Module can't have superclass" if @is_module
		@superclass = superclass
	end

	def include_module(ancestor)
		@included_modules << ancestor
	end
	def moduleType()
		@is_module?"Module":"Class"
	end
	def generate_java(output)
		output.write(
%Q[
/* Generated code - do not edit! */

package #{@package};

#{write_includes_string()}
public class #{@name}Definition extends #{moduleType}Definition {
private static final int #{constant_name} = #{@start_index};
private static final int STATIC = #{constant_name} + 0x100;
#{ string_method_constants() }
		
public #{@name}Definition(Ruby runtime) {
super(runtime);
}
protected Ruby#{moduleType} create#{@is_module?'Module':'Type'}(Ruby runtime) {
Ruby#{moduleType} result = runtime.#{string_class_definition}
#{	outstring = ""
	write_included_modules(outstring)
	outstring
}
return result;
}
#{ string_method_definitions }	
}

#{string_switch_callIndexed}

}
}
]

		)

	end
	def string_switch_callIndexed()
		outstring = "public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {\n"
		outstring << "switch (index) {\n"
		@class_methods.each {|m|
			m.generate_switch_case(outstring)
		}
		outstring << 
"
default :
Asserts.notReached();
return null;
}
"
	end
	def string_method_definitions()
		outstring = "protected void defineMethods(MethodContext context) {\n"
		@methods.each { |m|
		m.generate_creation(outstring)
		}
		outstring << "}\n"
		if @is_module
			outstring << "protected void defineModuleFunctions(ModuleFunctionsContext context) {\n"
		else
			outstring << "protected void defineSingletonMethods(SingletonMethodContext context) {\n"
		end
		@class_methods.each {|m|
		m.generate_creation(outstring)
		}
		outstring
	end
	def string_class_definition()
		outstring = %Q[define#{moduleType}("#{@name}"]
		if @is_module
			outstring << ");"
		else
			outstring << ", " << (@superclass == "none"? 'null': %Q[(RubyClass) runtime.getClasses().getClass("#{@superclass}"));])
		end
	end
	def string_method_constants()
		outstring = ""
		@methods.each {|m|
		m.generate_constant(outstring)
	}
		@class_methods.each {|m|
		m.generate_constant(outstring)
	}
		outstring
	end
	def write_included_modules(output)
		@included_modules.each {|ancestor|
		output << "result.includeModule(runtime.getClasses().getClass(\""
		output << ancestor
		output << "\"));\n"
	}
	end

	def write_includes(output)
		output.write(write_includes_string())
	end

	def write_includes_string()
		output = ""
		INCLUDES.each {|include|
		output += "import #{include};\n"
	}
		output
	end

	
	def constant_name
		@name.upcase
	end
end


class MethodGenerator
	attr :classes, false
	def initialize(input = nil)
		
		@classes = Hash.new
		if input == nil
			return
		end
		parseFile(input)
	end

	def parseFile(input)
		parser = Parser.new(input)
		class_description = parser.read_input
		@classes[  class_description.name ]= class_description
		class_description.compute_start_index(self)
		return class_description
	end
	def mkdir(iDir)
		if iDir == '.' || File.exist?(iDir)
			return
		end
		mkdir(File.dirname(iDir))
		puts "creating directory " + iDir
		Dir.mkdir(iDir)
	end
	def generate_java(class_description, iOutput = nil)
		if iOutput == nil
			filename = class_description.package.gsub(/\./, '/') + "/" + class_description.name + "Definition.java"
			#compute the directory where to put the java definition
			mkdir(File.dirname(filename))
			output = open(filename, 'w')
			puts "writing file " + filename
		else
			output = iOutput
		end
		class_description.generate_java(output)
		output.close
	end
	def generate(iOutput = nil)

		output = nil
		@classes.each { |key,class_description|
		generate_java(class_description, iOutput)
		@classes[key]= ClassProxy.new(class_description)
	}
	end
end

class ClassProxy
	def initialize(class_description)
		@lastMethodIndex = class_description.lastMethodIndex
	end
	def lastMethodIndex
		@lastMethodIndex
	end
end

class Parser

	def initialize(input)
		@input = input
	end

	def read_input
		class_description = ClassDescription.new

		method_count = nil
		methods = nil
		method_description_class = nil

		parser = LowlevelParser.new(@input)
		parser.on_tag_start("module") {|name, attributes|
		if attributes['type'] == "module"
			class_description.is_module = true
		end
	}
		parser.on_tag_content("name") {|text|
		class_description.name = text
	}
		parser.on_tag_content("superclass") {|text|
		class_description.superclass = text
	}
		parser.on_tag_content("includes") {|text|
		class_description.include_module(text)
	}
		parser.on_tag_content("implementation") {|text|
		class_description.implementation = text
	}
		parser.on_tag_start("instance-methods") {|name, attributes|
		methods = class_description.methods
		method_count = 0
		method_description_class = MethodDescription
	}
		parser.on_tag_start("class-methods") {|name, attributes|
		methods = class_description.class_methods
		method_count = 0
		method_description_class = StaticMethodDescription
	}
		parser.on_tag_start("method") {|name, attributes|
		method_count += 1
		methods << method_description_class.new(class_description,
		attributes['name'],
		method_count)
	}
		parser.on_tag_start("arity") {|name, attributes|
		if attributes.has_key?('optional')
			methods.last.optional = (attributes['optional'] == 'true')
		end
	}
		parser.on_tag_content("arity") {|text|
		methods.last.arity = text.to_i
	}
		parser.on_tag_content("java") {|text|
		methods.last.java_name = text
	}
		parser.on_tag_start("method-alias") {|name, attributes|
		original_name = attributes['original']
		original = methods.detect {|m| m.name == original_name }
		raise "missing definition: #{original_name}" if original.nil?
		name = attributes['name']
		methods << Alias.new(name, original)
	}
		parser.on_tag_start("undefine-method") {|name, attributes|
		name = attributes['name']
		methods << UndefineMethod.new(name)
	}
		parser.parse

		class_description
	end
end

class LowlevelParser

	def initialize(input)
		@saxparser = REXML::SAX2Parser.new(input)
	end

	def on_tag_start(name, &block)
		name.gsub!(/\-/, '\-')
		@saxparser.listen(:start_element, ['^' + name + '$']) {
		|uri, localname, qname, attributes|
		block.call(localname, attributes)
	}
	end

	def on_tag_content(name, &block)
		@saxparser.listen(:characters, ['^' + name + '$']) {
		|text|
		block.call(text)
	}
	end

	def parse
		@saxparser.parse
	end
end


$package = "org.jruby.internal.runtime.builtin.definitions"

if $0 == __FILE__
	def help()
		puts "help: methodgenerator.rb options
  options:
	  -d directory:  parses all xml file in this directory
	  -i file: 		 parses this file
	  -h help: 		 get this help
	  -p package: 	 package for the generated definition
		"
		exit
	end
	generator = nil	
	inputs = []
	while arg = ARGV.shift
		case arg
			when "-d"
				inputs = Dir[ARGV.shift + "/*.xml"]
			when "-i"
				inputs << ARGV.shift
			when "-h"
				help
			when "-p"
				$package = ARGV.shift
		else
			puts "unexpected argument #{arg}"
			help
		end
	end
	if inputs.size == 0
		help
	end
	generator = MethodGenerator.new()
	inputs.each { |file|
		open(file) { |input|
			puts "reading file " + file
			generator.parseFile(input)
			generator.generate()
		}
	}
	

end
