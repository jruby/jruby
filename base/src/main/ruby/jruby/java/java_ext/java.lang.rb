# NOTE: these Ruby extensions were moved to native code!
# - **org.jruby.javasupport.ext.JavaLang.java**
# - **org.jruby.javasupport.ext.JavaLangReflect.java**
# this file is no longer loaded but is kept to provide doc stubs

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
  # @return [Proc] calling #run when caled
  def to_proc
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # proc { self.run }
  end
end if false

# A `java.lang.Iterable` will act like a Ruby `Enumerable`.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Iterable.html
module Java::java::lang::Iterable
  include ::Enumerable

  # Ruby style `Enumerable#each` iteration for Java iterable types.
  # @return [Java::java::util::Iterable] self (since 9.1.3)
  # @return [Enumerator] if called without a block to yield to
  def each(&block)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # iter = iterator
    # yield(iter.next) while iter.hasNext
  end

  # Ruby style `Enumerable#each_with_index` for Java iterable types.
  # @return [Java::java::util::Iterable] self (since 9.1.3)
  # @return [Enumerator] if called without a block to yield to
  def each_with_index(&block)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # index = 0
    # iter = iterator
    # while iter.hasNext
    #   yield(iter.next, index)
    #   index += 1
    # end
  end

  # Re-defined `Enumerable#to_a`.
  # @return [Array]
  # @since 9.1.3
  def to_a
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end
  alias entries to_a

  # Re-defined `Enumerable#count`.
  # @return [Integer] matched elements count
  # @since 9.1.3
  def count(obj = nil, &block)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end
end if false

# *java.lang.Comparable* mixes in Ruby's `Comparable` support.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html
module Java::java::lang::Comparable
  include ::Comparable

  def <=>(a)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # return nil if a.nil?
    # compareTo(a)
  end
end if false

# Java's *java.lang.Throwable* (exception/error) classes resemble Ruby's `Exception`.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Throwable.html
class Java::java::lang::Throwable

  # @return [Array] the mapped stack-trace
  def backtrace
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # stack_trace.map(&:to_s)
  end

  # @note Noop as Java exceptions can not change their stack-trace.
  def set_backtrace(trace)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # Always a non-nil to follow Ruby's {Exception#message} conventions.
  # @note getMessage still returns nil, when no message was given for the Java exception!
  # @return [String]
  def message
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # getLocalizedMessage || ''
  end

  def to_s
    # message
  end

  def inspect
    # to_string
  end

  # Adds case compare against `NativeException` wrapped throwables.
  # @example
  #    begin
  #      java.lang.Integer.parseInt('gg', 16)
  #    rescue NativeException => ex
  #      expect( java.lang.NumberFormatException === ex ).to be true
  #    end
  # @return [true, false]
  def self.===(ex)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

end if false

# *java.lang.Character* represents an object wrapper for Java's *char* primitive.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Character.html
class Java::java::lang::Character

  # `java.lang.Character.isJavaIdentifierStart(char)`
  # @return [true, false]
  def self.java_identifier_start?(char)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # `java.lang.Character.isJavaIdentifierPart(char)`
  # @return [true, false]
  def self.java_identifier_part?(char)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

end if false

# *java.lang.Class*
# @note Only explicit (or customized) Ruby methods are listed here,
#       Java classes will have all of their Java methods available.
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/Class.html
# @todo likely to get revised!
class Java::java::lang::Class
  include ::Comparable
  # include ::JavaUtilities::ModifierShortcuts

  # @return [Class, Module] the proxy class (or module in case of an interface).
  def ruby_class
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # ::JRuby.runtime.java_support.get_proxy_class_from_cache(self)
  end

  # @return [String] the Java class name
  def to_s
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [String] `java.lang.Class#toString`
  def inspect
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [Java::java::io::InputStream]
  def resource_as_stream(name)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [String]
  def resource_as_string(name)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # resource_as_stream(name).to_io.read
  end

  # @return [true, false]
  def annotations?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # !annotations.empty?
  end

  # @return [true, false]
  def declared_annotations?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # !declared_annotations.empty?
  end

  def <=>(other)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # return nil unless other.class == java::lang::Class
    #
    # return  0 if self == other
    # return +1 if self.is_assignable_from(other)
    # return -1 if other.is_assignable_from(self)
  end

  def java_instance_methods
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  def declared_instance_methods
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  def java_class_methods
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  def declared_class_methods
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [true, false]
  # @since 9.1
  def anonymous?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [true, false]
  # @since 9.1
  def abstract?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [true, false]
  def public?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [true, false]
  def protected?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [true, false]
  def private?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [true, false]
  def final?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @private
  def static?
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

end if false

# *java.lang.ClassLoader*
# @see http://docs.oracle.com/javase/8/docs/api/java/lang/ClassLoader.html
class Java::java::lang::ClassLoader
  # @return [Java::java::io::InputStream]
  def resource_as_stream(name)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end

  # @return [String]
  def resource_as_string(name)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
    # resource_as_stream(name).to_io.read
  end

  def resource_as_url(name)
    # stub implemented in org.jruby.javasupport.ext.JavaLang.java
  end
end if false

# @see http://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Constructor.html
class Java::java::lang::reflect::Constructor

  def return_type
    nil
  end

  def argument_types
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
    # parameter_types
  end

  # @return [true, false]
  def public?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def protected?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def private?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def final?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @private
  def static?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

end if false

# @see http://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Method.html
class Java::java::lang::reflect::Method

  def return_type
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  def argument_types
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
    # parameter_types
  end

  def invoke_static(*args)
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
    # invoke(nil, *args)
  end

  # @return [true, false]
  # @since 9.1
  def abstract?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def public?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def protected?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def private?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def final?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def static?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

end if false

# @see http://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Field.html
class Java::java::lang::reflect::Field

  def value_type
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  def value(obj)
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
    # get(obj)
  end

  def set_value(obj, value)
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
    # set(obj, value)
  end

  def static_value
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
    # get(nil)
  end

  def set_static_value(value)
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
    # set(nil, value)
  end

  # @return [true, false]
  def public?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def protected?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def private?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def final?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

  # @return [true, false]
  def static?
    # stub implemented in org.jruby.javasupport.ext.JavaLangReflect.java
  end

end if false

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
end if false
