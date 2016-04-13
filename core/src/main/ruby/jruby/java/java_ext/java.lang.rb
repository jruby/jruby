# @private internal helper
module JavaUtilities::ModifierShortcuts
  # @private
  Modifier = java.lang.reflect.Modifier

  def public?
    Modifier.is_public(modifiers)
  end

  def protected?
    Modifier.is_protected(modifiers)
  end

  def private?
    Modifier.is_private(modifiers)
  end

  def final?
    Modifier.is_final(modifiers)
  end

  def static?
    Modifier.is_static(modifiers)
  end
end

# *java.lang.Runnable* instances allow for a `to_proc` conversion.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html
module Java::java::lang::Runnable
  def to_proc
    proc { self.run }
  end
end

# A `java.lang.Iterable` will act like a Ruby `Enumerable`.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Iterable.html
module Java::java::lang::Iterable
  include ::Enumerable

  def each
    iter = iterator
    yield(iter.next) while iter.hasNext
  end

  def each_with_index
    index = 0
    iter = iterator
    while iter.hasNext
      yield(iter.next, index)
      index += 1
    end
  end
end

# *java.lang.Comparable* mixes in Ruby's `Comparable` support.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html
module Java::java::lang::Comparable
  include ::Comparable

  def <=>(a)
    return nil if a.nil?
    compareTo(a)
  end
end

# Java's *java.lang.Throwable* (exception/errors) classes resemble Ruby's `Exception`.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Throwable.html
class Java::java::lang::Throwable

  def backtrace
    stack_trace.map(&:to_s)
  end

  # @note Noop as Java exceptions can not change trace.
  def set_backtrace(trace)
  end

  # Always a non-nil to follow Ruby's {Exception#message} conventions.
  # @note getMessage still returns nil, when no message was given for the Java exception!
  # @return [String]
  def message
    getLocalizedMessage || ''
  end

  def to_s
    message
  end

  def inspect
    to_string
  end

  class << self
    alias :old_eqq :===
    def ===(rhs)
      if (NativeException == rhs.class) && (java_class.assignable_from?(rhs.cause.java_class))
        true
      else
        old_eqq(rhs)
      end
    end
  end
end

class Java::java::lang::Character
  java_alias :isJavaIdentifierStart_char, :isJavaIdentifierStart, [Java::char]
  java_alias :isJavaIdentifierPart_char, :isJavaIdentifierPart, [Java::char]

  def self.java_identifier_start?(fixnum)
    isJavaIdentifierStart_char(fixnum);
  end

  def self.java_identifier_part?(fixnum)
    isJavaIdentifierPart_char(fixnum);
  end
end

# *java.lang.Class*
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Class.html
# @todo likely to get revised!
class Java::java::lang::Class
  include ::Comparable
  include ::JavaUtilities::ModifierShortcuts

  def ruby_class
    ::JRuby.runtime.java_support.get_proxy_class_from_cache(self)
  end

  alias to_s name

  def inspect
    "class #{name}"
  end

  def resource_as_string(name)
    resource_as_stream(name).to_io.read
  end

  alias annotation get_annotation

  def annotations?
    !annotations.empty?
  end

  def declared_annotations?
    !declared_annotations.empty?
  end

  alias annotation_present? is_annotation_present

  def <=>(other)
    return nil unless other.class == java::lang::Class

    return 0 if self == other
    return 1 if self.is_assignable_from(other)
    return -1 if other.is_assignable_from(self)
  end

  def java_instance_methods
    methods.select {|m| !Modifier.is_static(m.modifiers)}.freeze
  end

  def declared_instance_methods
    declared_methods.select {|m| !Modifier.is_static(m.modifiers)}.freeze
  end

  def java_class_methods
    methods.select {|m| Modifier.is_static(m.modifiers)}.freeze
  end

  def declared_class_methods
    declared_methods.select {|m| Modifier.is_static(m.modifiers)}.freeze
  end
end

# *java.lang.ClassLoader*
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/ClassLoader.html
class Java::java::lang::ClassLoader
  alias resource_as_stream get_resource_as_stream
  alias resource_as_url get_resource

  def resource_as_string(name)
    resource_as_stream(name).to_io.read
  end
end

class Java::java::lang::reflect::AccessibleObject
  include ::JavaUtilities::ModifierShortcuts

  alias inspect to_s
end

class Java::java::lang::reflect::Constructor
  def return_type
    nil
  end

  alias argument_types parameter_types
end

class Java::java::lang::reflect::Method
  def invoke_static(*args)
    invoke(nil, *args)
  end

  alias argument_types parameter_types
end

class Java::java::lang::reflect::Field
  alias value_type name
  alias value get
  alias set_value set

  def static_value
    get(nil)
  end

  def set_static_value(val)
    set(nil, val)
  end
end

Java::byte[].class_eval do
  def ubyte_get(index)
    byte = self[index]
    byte += 256 if byte < 0
    byte
  end

  def ubyte_set(index, value)
    value -= 256 if value > 127
    self[index] = value
  end
end
