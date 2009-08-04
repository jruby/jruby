require 'test/unit'

class TestObjectClassDefaultMethods < Test::Unit::TestCase
  METHODS = ["<", "<=", "<=>", "==", "===", "=~", ">", ">=", "__id__", "__send__", "allocate", "ancestors", "autoload", "autoload?", "class", "class_eval", "class_variable_defined?", "class_variables", "clone", "const_defined?", "const_get", "const_missing", "const_set", "constants", "display", "dup", "enum_for", "eql?", "equal?", "extend", "freeze", "frozen?", "hash", "id", "include?", "included_modules", "inspect", "instance_eval", "instance_exec", "instance_method", "instance_methods", "instance_of?", "instance_variable_defined?", "instance_variable_get", "instance_variable_set", "instance_variables", "is_a?", "kind_of?", "method", "method_defined?", "methods", "module_eval", "name", "new", "nil?", "object_id", "private_class_method", "private_instance_methods", "private_method_defined?", "private_methods", "protected_instance_methods", "protected_method_defined?", "protected_methods", "public_class_method", "public_instance_methods", "public_method_defined?", "public_methods", "respond_to?", "send", "singleton_methods", "superclass", "taint", "tainted?", "tap", "to_a", "to_enum", "to_s", "type", "untaint"]

  def test_no_rogue_methods_on_object_class
    assert_equal METHODS, Object.methods.sort
  end
end
