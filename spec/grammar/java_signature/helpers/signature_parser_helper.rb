require 'java'

java_import org.jruby.ast.java_signature.PrimitiveTypeNode
java_import org.jruby.ast.java_signature.ArrayTypeNode

java_import org.jruby.parser.JavaSignatureParser

java_import java.io.ByteArrayInputStream

BYTE = PrimitiveTypeNode::BYTE
SHORT = PrimitiveTypeNode::SHORT
INT = PrimitiveTypeNode::INT
LONG = PrimitiveTypeNode::LONG
CHAR = PrimitiveTypeNode::CHAR
FLOAT = PrimitiveTypeNode::FLOAT
DOUBLE = PrimitiveTypeNode::DOUBLE
BOOLEAN = PrimitiveTypeNode::BOOLEAN
VOID = PrimitiveTypeNode::VOID

Override = java.lang.Override

class Object
  def signature(string)
    bytes = string.to_java.bytes
    JavaSignatureParser.parse ByteArrayInputStream.new(bytes)
  end

  def arrayOf(a_type)
    ArrayTypeNode.new(a_type)
  end
end

class SimpleConstructorSignatureMatcher
  attr_reader :errors

  def initialize(*args)
    @args, @errors = parse_args(*args), []
  end

  def parse_args(*args)
    # FIXME: Error for other lengths
    case args.length
    when 2 then # name, params
      [[], args[0], args[1], []]
    when 3 then # modifiers, name, params
      [args[0], args[1], args[2], []]
    when 4 then # modifiers, name, params, throws
      [args[0], args[1], args[2], args[3]]
    end
  end

  def match_type?(ast_type, expected_type)
    if expected_type.kind_of? Symbol
      ast_type.to_s == expected_type.to_s
    else
      ast_type == expected_type
    end
  end

  def match_parameters?(parameters, expected)
    return false if parameters.size != expected.size

    0.upto(parameters.size - 1) do |i|
      return false unless match_type? parameters[i].get_type, expected[i]
    end
    true
  end

  def matches?(ast)
    modifiers, name, parameters, throws = *@args
    @errors << ['modifiers', ast.modifiers, modifiers] unless ast.modifiers.equals? modifiers
    @errors << ['name', ast.name, name] unless ast.name == name
    @errors << ['parameters', ast.parameters, parameters] unless match_parameters? ast.parameters, parameters
    @errors << ['throws', ast.throws, throws] unless ast.throws.equals? throws
    @errors.length == 0
  end

  def failure_message
    @errors.inject('') do |memo, item|
      memo += "#{item[0]}: "
      # FIXME: Add nicer output when array mismatches
      memo += "#{item[1].inspect} to equal #{item[2].inspect}\n"
      memo
    end
  end

  def negative_failure_message
    @errors.inject('') do |memo, item|
      memo += "#{item[0]}: "
      # FIXME: Add nicer output when array mismatches
      memo += "#{item[1].inspect} to not equal #{item[2].inspect}\n"
      memo
    end
  end
end

class SimpleSignatureMatcher
  attr_reader :errors

  def initialize(*args)
    @args, @errors = parse_args(*args), []
  end

  def parse_args(*args)
    # FIXME: Error for other lengths
    case args.length
    when 3 then # return_type, name, params
      [[], args[0], args[1], args[2], []]
    when 4 then # modifiers, return_type, name, params
      [args[0], args[1], args[2], args[3], []]
    when 5 then # modifiers, return_type, name, params, throws
      [args[0], args[1], args[2], args[3], args[4]]
    end
  end

  def match_type?(ast_type, expected_type)
    if expected_type.kind_of? Symbol
      ast_type.to_s == expected_type.to_s
    else
      ast_type == expected_type
    end
  end

  def match_parameters?(parameters, expected)
    return false if parameters.size != expected.size

    0.upto(parameters.size - 1) do |i|
      return false unless match_type? parameters[i].get_type, expected[i]
    end
    true
  end

  def modifiers_match?(modifiers, expected_list)
    modifiers.each_with_index do |modifier, i|
      return false if modifier != expected_list[i]
    end
    true
  end

  def matches?(ast)
    modifiers, return_type, name, parameters, throws = *@args
    # Mildly brittle to depend on toString, but unlikely to change.
    expected_modifiers = modifiers.map(&:to_s)
    actual_modifiers = ast.modifiers.map(&:to_s)

    @errors << ['modifiers', actual_modifiers, expected_modifiers] unless modifiers_match? actual_modifiers, expected_modifiers
    @errors << ['return type', ast.return_type, return_type] unless match_type? ast.return_type.to_s, return_type.to_s
    @errors << ['name', ast.name, name] unless ast.name == name
    @errors << ['parameters', ast.parameters, parameters] unless match_parameters? ast.parameters, parameters
    @errors << ['throws', ast.throws, throws] unless ast.throws.equals? throws
    @errors.length == 0
  end

  def failure_message
    @errors.inject('') do |memo, item|
      memo += "#{item[0]}: "
      # FIXME: Add nicer output when array mismatches
      memo += "#{item[1].inspect} to equal #{item[2].inspect}\n"
      memo
    end
  end

  def negative_failure_message
    @errors.inject('') do |memo, item|
      memo += "#{item[0]}: "
      # FIXME: Add nicer output when array mismatches
      memo += "#{item[1].inspect} to not equal #{item[2].inspect}\n"
      memo
    end
  end
end
 
class Object
  def have_signature(*args)
    SimpleSignatureMatcher.new(*args)
  end

  def have_constructor_signature(*args)
    SimpleConstructorSignatureMatcher.new(*args)
  end
end
