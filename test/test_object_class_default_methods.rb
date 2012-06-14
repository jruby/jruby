require 'test/unit'

class TestObjectClassDefaultMethods < Test::Unit::TestCase
  METHODS = %w(
    <
    <=
    <=>
    ==
    ===
    =~
    >
    >=
    __id__
    __send__
    allocate
    ancestors
    autoload
    autoload?
    class
    class_eval
    class_exec
    class_variable_defined?
    class_variables
    clone
    const_defined?
    const_get
    const_missing
    const_set
    constants
    display
    dup
    enum_for
    eql?
    equal?
    extend
    freeze
    frozen?
    hash
    id
    include?
    included_modules
    inspect
    instance_eval
    instance_exec
    instance_method
    instance_methods
    instance_of?
    instance_variable_defined?
    instance_variable_get
    instance_variable_set
    instance_variables
    is_a?
    kind_of?
    method
    method_defined?
    methods
    module_eval
    module_exec
    name
    new
    nil?
    object_id
    private_class_method
    private_instance_methods
    private_method_defined?
    private_methods
    protected_instance_methods
    protected_method_defined?
    protected_methods
    public_class_method
    public_instance_methods
    public_method_defined?
    public_methods
    respond_to?
    send
    singleton_methods
    superclass
    taint
    tainted?
    tap
    to_a
    to_enum
    to_s
    type
    untaint
  )

  JAVA_METHODS = %w(
    method_added
    handle_different_imports
    include_class
    java_kind_of?
    java_signature
    com
    to_java
    java_annotation
    org
    java_implements
    java
    java_package
    java_name
    java_require
    javax
  )

  def test_no_rogue_methods_on_object_class
    rogue_methods = Object.methods - METHODS
    # reject Java methods, since we have started loading it for core functionality
    rogue_methods -= JAVA_METHODS

    rogue_methods.reject!{|m| m =~ /^__.+__$/}

    assert rogue_methods.empty?, "Rogue methods found: #{rogue_methods.inspect}"
  end
end
