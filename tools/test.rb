
require 'test/unit'

require 'methodgenerator'


def stream_string
  output = ""
  def output.write(s)
    self.<<(s)
  end
  output
end


class TestGenerator < Test::Unit::TestCase

  def test_generator
    output = stream_string

    generator = nil
    open("test_data.xml") do |file|
      generator = MethodGenerator.new(file, 'some.place.to.put.definitions')
      generator.generate(output)
    end

    expected_output = nil
    open("test_data.expected") do |file|
      expected_output = file.gets(nil)
    end

    expected_output = expected_output.split
    output = output.split
    assert_equal(expected_output.length, output.length)
    expected_output.each_with_index {|line, i|
      assert_equal(line, output[i])
    }
  end

  def test_class
    c = ClassDescription.new
    c.name = "Foo"
    assert_equal("FOO", c.constant_name)

    c.add_ancestor("Froboz")
    output = stream_string
    c.write_ancestors(output)
    assert_equal("result.includeModule(runtime.getClasses().getClass(\"Froboz\"));\n",
                 output)
  end

  def test_parser
    xml = '<?xml version="1.0"?>' +
      '<module type="class"><ancestor>Foo</ancestor>' +
      '</module>' + "\n"

    p = Parser.new(xml)
    description = p.read_input
    assert_equal(["Foo"], description.ancestors)
  end

  def test_method_constant
    generator = Object.new
    def generator.constant_name
      "FOO"
    end

    output = stream_string
    m = MethodDescription.new(generator, "swamp", 12)
    assert_equal("swamp", m.name)
    m.generate_constant(output)
    assert_equal("public static final int SWAMP = FOO | 12;\n", output)

    assert_equal("swamp", m.name)
    
    m.java_name = "swamp_stuff"
    output = stream_string
    m.generate_constant(output)
    assert_equal("public static final int SWAMP_STUFF = FOO | 12;\n", output)

    m.arity = 3
    m.optional = true
    output = stream_string
    m.generate_creation(output)
    assert_equal('context.createOptional("swamp", SWAMP_STUFF, 3);' + "\n", output)
  end

  def test_method_creation
    generator = Object.new
    m = MethodDescription.new(generator, "swamp", 12)

    output = stream_string
    m.arity = 3
    m.generate_creation(output)
    assert_equal('context.create("swamp", SWAMP, 3);' + "\n", output)

    output = stream_string
    m.optional = true
    m.generate_creation(output)
    assert_equal('context.createOptional("swamp", SWAMP, 3);' + "\n", output)

    m.java_name = "swamp_stuff"

    assert_equal("swamp", m.name)
  end

  def test_static_method_constant
    generator = Object.new
    def generator.constant_name
      "FOO"
    end

    output = stream_string
    m = StaticMethodDescription.new(generator, "swamp", 12)
    assert_equal("swamp", m.name)
    m.generate_constant(output)
    assert_equal("public static final int SWAMP = STATIC | 12;\n", output)
  end

  def test_static_method_switch
    generator = Object.new
    def generator.implementation
      "org.stuff.Foo"
    end

    m = StaticMethodDescription.new(generator, "swamp", 12)
    output = stream_string
    m.generate_switch_case(output)
    assert_equal("case SWAMP :\nreturn org.stuff.Foo.swamp(receiver);\n", output)

    m.arity = 2
    output = stream_string
    m.generate_switch_case(output)
    assert_equal("case SWAMP :\nreturn org.stuff.Foo.swamp(receiver, args[0], args[1]);\n", output)

    m.optional = true
    output = stream_string
    m.generate_switch_case(output)
    assert_equal("case SWAMP :\nreturn org.stuff.Foo.swamp(receiver, args);\n", output)
  end

  def test_method_alias
    generator = Object.new
    original = MethodDescription.new(generator, "hello", 7)
    original.java_name = "hello_java"

    m = Alias.new("xyz", original)

    output = stream_string
    m.generate_constant(output)
    assert(output.empty?)

    output = stream_string
    m.generate_switch_case(output)
    assert(output.empty?)

    output = stream_string
    m.generate_creation(output)
    assert_equal('context.create("xyz", HELLO_JAVA, 0);' + "\n", output)

    original.optional = true

    output = stream_string
    m.generate_creation(output)
    assert_equal('context.createOptional("xyz", HELLO_JAVA, 0);' + "\n", output)  end

  def test_undefine_method
    m = UndefineMethod.new("froboz")

    output = stream_string
    m.generate_constant(output)
    assert(output.empty?)

    output = stream_string
    m.generate_creation(output)
    assert_equal('context.undefineMethod("froboz");' + "\n", output)
  end
end
