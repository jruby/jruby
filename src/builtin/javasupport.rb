###### BEGIN LICENSE BLOCK ######
# Version: CPL 1.0/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Common Public
# License Version 1.0 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.eclipse.org/legal/cpl-v10.html
#
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
#
# Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
# Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
# Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
# Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
# Copyright (C) 2006 Michael Studman <me@michaelstudman.com>
# Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
# Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
# Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
# 
# Alternatively, the contents of this file may be used under the terms of
# either of the GNU General Public License Version 2 or later (the "GPL"),
# or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the CPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the CPL, the GPL or the LGPL.
###### END LICENSE BLOCK ######

# JavaProxy is a base class for all Java Proxies.  A Java proxy is a high-level abstraction
# that wraps a low-level JavaObject with ruby methods capable of dispatching to the JavaObjects
# native java methods.  
class JavaProxy
  class << self
    attr :java_class, true

    # Allocate can collide with Java static methods named allocate, so
    # we shouldn't ever use that.
    alias jallocate! allocate

    # Allocate a new instance for the provided java_object.  This is like a second 'new' to
    # by-pass any 'initialize' methods we may have created for the proxy class (we already
    # have the instance for the proxy...We don't want to re-create it).
    def new_instance_for(java_object)
      new_instance = jallocate!
      new_instance.java_object = java_object
      new_instance
    end
    
    # Carry the Java class as a class variable on the derived classes too, otherwise 
    # JavaProxy.java_class won't work.
    def inherited(subclass)
      subclass.java_class = self.java_class unless subclass.java_class
      super
    end
    
    def singleton_class
      class << self; self; end 
    end

    # If the proxy class itself is passed as a parameter this will be called by Java#ruby_to_java    
    def to_java_object
      self.java_class
    end

    def [](*args)
      if args.length > 0
        # array creation should use this variant
        ArrayJavaProxyCreator.new(java_class,*args)      
      else
        # keep this variant for kind_of? testing
      JavaUtilities.get_proxy_class(java_class.array_class)
    end
    end
    
    def setup
      unless java_class.array?
        setup_attributes
        setup_class_methods
        setup_constants
        setup_inner_classes
        setup_instance_methods
      end
    end
    
    def setup_attributes
      instance_methods = java_class.java_instance_methods.collect! {|m| m.name}
      java_class.fields.select {|field| field.public? && !field.static? }.each do |attr|
        name = attr.name
      
        # Do not add any constants that have the same name as an existing method
        next if instance_methods.detect {|m| m == name }

        class_eval do
          define_method(name) do |*args| Java.java_to_ruby(attr.value(@java_object)); end
        end

        next if attr.final?

        class_eval do
          define_method("#{name}=") do |*args|
            Java.java_to_ruby(attr.set_value(@java_object, Java.ruby_to_java(args.first)))
          end
        end
      end
    end

    def setup_class_methods
      java_class.java_class_methods.select { |m| m.public? }.group_by { |m| m.name 
      }.each do |name, methods|
        if methods.length == 1
          method = methods.first
          singleton_class.send(:define_method, name) do |*args|
            args.collect! { |v| Java.ruby_to_java(v) }
            Java.java_to_ruby(method.invoke_static(*args))
          end
        else
          singleton_class.send(:define_method, name) do |*args|
            args.collect! { |v| Java.ruby_to_java(v) }
            Java.java_to_ruby(JavaUtilities.matching_method(methods, args).invoke_static(*args))
          end
        end
        singleton_class.instance_eval do
          alias_method name.gsub(/([a-z])([A-Z])/, '\1_\2').downcase, name
        end
      end
    end
    
    def setup_constants
      fields = java_class.fields
      class_methods = java_class.java_class_methods.collect! { |m| m.name } 

      fields.each do |field|
        if field.static? and field.final? and JavaUtilities.valid_constant_name?(field.name)
          const_set(field.name, Java.java_to_ruby(field.static_value))
        elsif (field.static? and field.final? and !JavaUtilities.valid_constant_name?(field.name)) or
            (field.public? and field.static? and !field.final? && !JavaUtilities.valid_constant_name?(field.name))
            
          next if class_methods.detect {|m| m == field.name } 
          class_eval do
            singleton_class.send(:define_method, field.name) do |*args|
              Java.java_to_ruby(java_class.field(field.name).static_value)
            end
          end

        end
      end
    end

    def setup_inner_classes
      # the select block filters out anonymous inner classes ($1 and friends)
      # these have empty simple names, which don't really work as constant names
      java_class.declared_classes.select{|c| !c.simple_name.empty?}.each do |clazz|
        inner_class = Java::JavaClass.for_name(clazz.name)
        JavaUtilities.create_proxy_class(clazz.simple_name.intern, inner_class, self)
      end
    end
    
    def setup_instance_methods
      java_class.define_instance_methods_for_proxy(self)
    end
  end
  
  attr :java_object, true

  def java_class
    self.class.java_class
  end

  def ==(rhs)
    java_object == rhs
  end
  
  def to_s
    java_object.to_s
  end

  def eql?(rhs)
    self == rhs
  end
  
  def equal?(rhs)
    java_object.equal?(rhs)
  end
  
  def hash()
    java_object.hash()
  end
  
  def to_java_object
    java_object
  end

  def synchronized
    java_object.synchronized { yield }
  end
end

# This class enables the syntax arr = MyClass[n][n]...[n].new
class ArrayJavaProxyCreator
  def initialize(java_class,*args)
    @java_class = java_class
    @dimensions = []
    extract_dimensions(args)
      end
  
  def [](*args)
    extract_dimensions(args)
    self
    end
  
  private
  def extract_dimensions(args)
    unless args.length > 0
      raise ArgumentError,"empty array dimensions specified"    
    end
    args.each do |arg|
      unless arg.kind_of?(Fixnum)
        raise ArgumentError,"array dimension length must be Fixnum"    
      end
      @dimensions << arg
    end  
    end
  public
  
  def new(fill_value=nil)
    array = @java_class.new_array(@dimensions)
    array_class = @java_class.array_class
    (@dimensions.length-1).times do
      array_class = array_class.array_class    
    end
    proxy_class = JavaUtilities.get_proxy_class(array_class)
    proxy = proxy_class.new(array)
    if fill_value
      converter = JavaArrayUtilities.get_converter(@java_class)
      JavaArrayUtilities.fill_array(proxy,@dimensions,converter.call(fill_value))
    end
    proxy  
  end
end

class ArrayJavaProxy < JavaProxy
  include Enumerable

  class << self  
    alias_method :new_proxy, :new

    # the 'size' variant should be phased out ASAP
    def new(java_array_or_size, fill_value=nil)
      proxy = new_proxy
      if java_array_or_size.kind_of?(Java::JavaArray)
        proxy.java_object = java_array_or_size
        return proxy      
      end
      puts " ** Warning: the 'ClassName[].new(size)' form is deprecated; use ClassName[size].new"
      # sort out the mess of the previous approach
      size = java_array_or_size
      component_type = proxy.java_class.component_type
      component_type = component_type.component_type while component_type.array?
      array = component_type.new_array(size)
      # get the right proxy class for the number of array dimensions
      array_class = component_type.array_class
      if size.kind_of?(Array)
        (size.length-1).times do
          array_class = array_class.array_class    
        end
      end
      proxy_class = JavaUtilities.get_proxy_class(array_class)
      proxy = proxy_class.new(array)
      if fill_value
        converter = JavaArrayUtilities.get_converter(component_type)
        JavaArrayUtilities.fill_array(proxy,size,converter.call(fill_value))
      end
      proxy
    end
    
  end

  def length()
    java_object.length
  end
  
  def [](index)
    Java.java_to_ruby(java_object[index])
  end
  
  def []=(index, value)
    #java_object[index] = Java.ruby_to_java(value)

    # I don't know if this (instance var) will cause any problems. If so,
    # I can call JavaArrayUtilities.convert_to_type on every call to []=, but
    # I'd rather keep the converter locally for better performance. -BD
    @converter ||= JavaArrayUtilities.get_converter(java_class.component_type)
    java_object[index] = @converter.call(value)
  end
  
  def each()
    block = Proc.new
    for index in 0...java_object.length
      block.call(self[index])
    end
  end
  
  def to_a
    JavaArrayUtilities.java_to_ruby(self)
  end

  alias_method :to_ary, :to_a

end

class InterfaceJavaProxy < JavaProxy
  class << self  
    alias_method :new_proxy, :new

    def new(*args, &block)
      proxy = new_proxy(*args, &block)
      proxy.java_object = Java.new_proxy_instance(proxy.class.java_class) { |proxy2, method, *args|
        args.collect! { |arg| Java.java_to_ruby(arg) }
        Java.ruby_to_java(proxy.send(method.name, *args))
      }
      proxy
    end
    
    def +(other)
      MultipleInterfaceJavaProxy.new(lambda{|*args| new_proxy(*args)}, self, other)
    end
    
    alias_method :old_eqq, :===
    
    def ===(other)
      if other.respond_to?(:java_object)
        other.java_object.java_class.interfaces.include?(self.java_class)
      else
        old_eqq(other)
      end
    end
  end
    
  def self.impl(*meths, &block)
    block = lambda {|*args| send(:method_missing, *args) } unless block

    Class.new(self) do
      define_method(:method_missing) do |name, *args|
        return block.call(name, *args) if meths.empty? || meths.include?(name)
        super
      end
    end.new
  end
end

class MultipleInterfaceJavaProxy
  attr_reader :interfaces
    
  def initialize(creator, *args)
    @creator = creator
    @interfaces = args.map{ |v| into_arr(v) }.flatten
  end

  def <<(other)
    @interfaces += into_arr(other)
  end

  def +(other)
    MultipleInterfaceJavaProxy.new @creator, *(@interfaces + into_arr(other))
  end
    
  def new(*args, &block)
    @interfaces.freeze unless @interfaces.frozen?
    proxy = @creator.call(*args)
    proxy.java_object = Java.new_proxy_instance(*@interfaces) { |proxy2, method, *args|
      args.collect! { |arg| Java.java_to_ruby(arg) }
      Java.ruby_to_java(proxy.__jsend!(method.name, *args))
    }
    proxy
  end

  def ===(other)
    if other.respond_to?(:java_object)
      (@interfaces - other.java_object.java_class.interfaces) == []
    else
      super
    end
  end

  private
  def into_arr(other)
    case other
      when MultipleInterfaceJavaProxy: other.interfaces
      else [other.java_class]
    end
  end
end

class ConcreteJavaProxy < JavaProxy
  class << self
    alias_method :new_proxy, :new

    def new(*args,&block)
      proxy = new_proxy *args,&block
      proxy.__jcreate!(*args) unless proxy.java_object
      proxy
    end
  end
  
  def __jcreate!(*args)
    constructors = self.java_class.constructors.select {|c| c.arity == args.length }
    raise NameError.new("wrong # of arguments for constructor") if constructors.empty?
    args.collect! { |v| Java.ruby_to_java(v) }
    self.java_object = JavaUtilities.matching_method(constructors, args).new_instance(*args)
  end
  
  def initialize(*args, &block)
    __jcreate!(*args)
  end
end

module JavaUtilities
  @proxy_classes = {}
  @proxy_extenders = []
  
  def JavaUtilities.add_proxy_extender(extender)
    @proxy_extenders << extender
    # Already loaded proxies should be extended if they qualify
    @proxy_classes.values.each {|proxy_class| extender.extend_proxy(proxy_class) }
  end
  
  def JavaUtilities.extend_proxy(java_class_name, &block)
	add_proxy_extender JavaInterfaceExtender.new(java_class_name, &block)
  end

  def JavaUtilities.valid_constant_name?(name)
    return false if name.empty?
    first_char = name[0..0]
    first_char == first_char.upcase && first_char != first_char.downcase
  end

  def JavaUtilities.get_proxy_class(java_class)
    java_class = Java::JavaClass.for_name(java_class) if java_class.kind_of?(String)
    class_id = java_class.object_id

    java_class.synchronized do
      unless @proxy_classes[class_id]
        if java_class.interface?
          base_type = InterfaceJavaProxy
        elsif java_class.array?
          base_type = ArrayJavaProxy
        else
          base_type = ConcreteJavaProxy
        end
        
        proxy_class = Class.new(base_type) { 
          self.java_class = java_class 
          if base_type == ConcreteJavaProxy
            class << self
              def inherited(subclass)
                super
                JavaUtilities.setup_java_subclass(subclass, java_class)
              end
            end
          end
        }
        @proxy_classes[class_id] = proxy_class
        # We do not setup the proxy before we register it so that same-typed constants do
        # not try and create a fresh proxy class and go into an infinite loop
        proxy_class.setup
        @proxy_extenders.each {|e| e.extend_proxy(proxy_class)}
      end
    end
    @proxy_classes[class_id]
  end

  def JavaUtilities.setup_java_subclass(subclass, java_class)
  
    # add new class-variable to hold the JavaProxyClass instance
    subclass.class.send :attr, :java_proxy_class, true
    
    subclass.send(:define_method, "__jcreate!") {|*args|
      constructors = self.class.java_proxy_class.constructors.select {|c| c.arity == args.length }
      raise NameError.new("wrong # of arguments for constructor") if constructors.empty?
      args.collect! { |v| Java.ruby_to_java(v) }
      self.java_object = JavaUtilities.matching_method(constructors, args).new_instance(args) { |proxy, method, *args|
        args.collect! { |arg| Java.java_to_ruby(arg) } 
        result = __jsend!(method.name, *args)
        Java.ruby_to_java(result)
      } 
    }
		
    subclass.send(:define_method, "setup_instance_methods") {
      self.java_proxy_class.define_instance_methods_for_proxy(subclass)
    }
    
    subclass.java_proxy_class = Java::JavaProxyClass.get(java_class)
  end

  def JavaUtilities.get_java_class(name)
    begin
      return Java::JavaClass.for_name(name)
    rescue NameError
      return nil
    end
  end
  
  def JavaUtilities.create_proxy_class(constant, java_class, mod)
    mod.const_set(constant.to_s, get_proxy_class(java_class))
  end
  
  def JavaUtilities.print_class(java_type, indent="")
     while (!java_type.nil? && java_type.name != "java.lang.Class")
        puts "#{indent}Name:  #{java_type.name}, access: #{ JavaUtilities.access(java_type) }  Interfaces: "
        java_type.interfaces.each { |i| print_class(i, "  #{indent}") }
        puts "#{indent}SuperClass: "
        print_class(java_type.superclass, "  #{indent}")
        java_type = java_type.superclass
     end
  end

  def JavaUtilities.access(java_type)
    java_type.public? ? "public" : (java_type.protected? ? "protected" : "private")
  end

  # Wrap a low-level java_object with a high-level java proxy  
  def JavaUtilities.wrap(java_object)
    get_proxy_class(java_object.java_class).new_instance_for(java_object)
  end

  @@primitive_matches = {
    'int'     => ['java.lang.Integer','java.lang.Long','java.lang.Short','java.lang.Character'],
    'long'    => ['java.lang.Integer','java.lang.Long','java.lang.Short','java.lang.Character'],
    'short'   => ['java.lang.Integer','java.lang.Long','java.lang.Short','java.lang.Character'],
    'char'    => ['java.lang.Integer','java.lang.Long','java.lang.Short','java.lang.Character'],
    'float'   => ['java.lang.Float','java.lang.Double'],
    'double'  => ['java.lang.Float','java.lang.Double'],
    'boolean' => ['java.lang.Boolean'] }

  def JavaUtilities.primitive_match(t1,t2)
    if t1.primitive?
      return (matches = @@primitive_matches[t1.inspect]) && matches.include?(t2.inspect)
    end
    return true
  end
  
  def JavaUtilities.matching_method(methods, args)
    @match_cache ||= {}

    arg_types = args.collect {|a| a.java_class }

    @match_cache[methods] ||= {}
    method = @match_cache[methods][arg_types]
    return method if method
    
    notfirst = false
    2.times do
      methods.each do |method|
        types = method.argument_types
        # Exact match
        return @match_cache[methods][arg_types] = method if types == arg_types
        
        # Compatible (by inheritance)
        if (types.length == arg_types.length)
          match = true
          0.upto(types.length - 1) do |i|
            match = false unless types[i].assignable_from?(arg_types[i]) && (notfirst || primitive_match(types[i],arg_types[i]))
          end
          return @match_cache[methods][arg_types] = method if match
        end
      end
      notfirst = true
    end

    name = methods.first.kind_of?(Java::JavaConstructor) || methods.first.kind_of?(Java::JavaProxyConstructor) ? 
      "constructor" : "method '" + methods.first.name + "'"
    raise NameError.new("no " + name + " with arguments matching " + arg_types.inspect)
  end

  @primitives = {
    :boolean => true,
    :byte => true,
    :char => true,
    :short => true,
    :int => true,
    :long => true,
    :float => true,
    :double => true  
  }
  def JavaUtilities.is_primitive_type(sym)
    @primitives[sym]  
  end
end

module JavaArrayUtilities
  class ProxyRef
    def initialize(class_name)
      @class_name = class_name
    end
    def new(*args)
      proxy.new(*args).java_object
    end
    def proxy
      @proxy ||= JavaUtilities.get_proxy_class(@class_name)
    end
    def name
      @class_name    
    end
    def valueOf(*args)
      proxy.valueOf(*args).java_object
    end
  end
  Jboolean = ProxyRef.new('boolean')
  Jbyte = ProxyRef.new('byte')
  Jchar = ProxyRef.new('char')
  Jshort = ProxyRef.new('short')
  Jint = ProxyRef.new('int')
  Jlong = ProxyRef.new('long')
  Jfloat = ProxyRef.new('float')
  Jdouble = ProxyRef.new('double')
  JBoolean = ProxyRef.new('java.lang.Boolean')
  JByte = ProxyRef.new('java.lang.Byte')
  JCharacter = ProxyRef.new('java.lang.Character')
  JShort = ProxyRef.new('java.lang.Short')
  JInteger = ProxyRef.new('java.lang.Integer')
  JLong = ProxyRef.new('java.lang.Long')
  JFloat = ProxyRef.new('java.lang.Float')
  JDouble = ProxyRef.new('java.lang.Double')
  JBigDecimal = ProxyRef.new('java.math.BigDecimal')
  JBigInteger = ProxyRef.new('java.math.BigInteger')
  JObject = ProxyRef.new('java.lang.Object')
  JString = ProxyRef.new('java.lang.String')
  JDate = ProxyRef.new('java.util.Date')
  JOFalse = Java.ruby_to_java(false)
  JOTrue = Java.ruby_to_java(true)
  JIntegerMin = -2147483648
  JIntegerMax = 2147483647
  JLongMin = -9223372036854775808
  JLongMax = 9223372036854775807
  SBoolean = 'java.lang.Boolean'.to_sym
  SByte = 'java.lang.Byte'.to_sym
  SCharacter = 'java.lang.Character'.to_sym
  SShort = 'java.lang.Short'.to_sym
  SInteger = 'java.lang.Integer'.to_sym
  SLong = 'java.lang.Long'.to_sym
  SFloat = 'java.lang.Float'.to_sym
  SDouble = 'java.lang.Double'.to_sym
  SBigDecimal = 'java.math.BigDecimal'.to_sym
  SBigInteger = 'java.math.BigInteger'.to_sym
  SObject = 'java.lang.Object'.to_sym
  SString = 'java.lang.String'.to_sym
  SDate = 'java.util.Date'.to_sym
  # *very* loose/eager conversion rules in place here, can tighten them
  # up if need be. -BD
  # the order of evaluation in the converters is important, want to 
  # check the most probable first, then dispense with any unidentified 
  # non-Ruby classes before calling to_i/to_f/to_s. -BD
  @converters = {
    :boolean => Proc.new {|val| 
      if val == false || val.nil?
        JOFalse
      elsif val == true
        JOTrue
      elsif val.kind_of?(Numeric)
        val.to_i == 0 ? JOFalse : JOTrue
      elsif val.kind_of?(String)
        JBoolean.new(val)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        val.to_i == 0 ? JOFalse : JOTrue
      else
        Java.ruby_to_java(val)
      end
    },
    :byte => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JByte.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JByte.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :char => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JCharacter.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JCharacter.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :short => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JShort.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JShort.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :int => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JInteger.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JInteger.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :long => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JLong.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JLong.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :float => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JFloat.new(val.to_f)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_f)
        JFloat.new(val.to_f)
      else
        Java.ruby_to_java(val)
      end
    },
    :double => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JDouble.new(val.to_f)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_f)
        JDouble.new(val.to_f)
      else
        Java.ruby_to_java(val)
      end
    },
    :decimal => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JBigDecimal.valueOf(val)
      elsif val.kind_of?(String)
        JBigDecimal.new(val)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_f)
        JBigDecimal.valueOf(val.to_f)
      elsif val.respond_to?(:to_i)
        JBigDecimal.valueOf(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :big_int => Proc.new {|val|
      if val.kind_of?(Integer)
        JBigInteger.new(val.to_s)
      elsif val.kind_of?(Numeric)
        JBigInteger.new(val.to_i.to_s)
      elsif val.kind_of?(String)
        JBigInteger.new(val)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JBigInteger.new(val.to_i.to_s)
      else
        Java.ruby_to_java(val)
      end
    },
    :object => Proc.new {|val|
      if val.kind_of?(Integer)
        if val >= JIntegerMin && val <= JIntegerMax
          JInteger.new(val)
        elsif val >= JLongMin && val <= JLongMax
          JLong.new(val)
        else
          JBigInteger.new(val.to_s)
        end
      elsif val.kind_of?(Float)
        JDouble.new(val)
      else
        Java.ruby_to_java(val)
      end
    },
    :string => Proc.new {|val|
      if val.kind_of?(String)
        JString.new(val)
      elsif val.kind_of?(JavaProxy)
        if val.respond_to?(:toString)
          JString.new(val.toString)
        else
          Java.ruby_to_java(val)
        end
      elsif val.respond_to?(:to_s)
        JString.new(val.to_s)
      else
        Java.ruby_to_java(val)
      end
    },
  }
  @converters['boolean'] = @converters['java.lang.Boolean'] = @converters[:Boolean] =
    @converters[SBoolean] = @converters[:boolean]
  @converters['byte'] = @converters['java.lang.Byte'] = @converters[:Byte] =
    @converters[SByte] = @converters[:byte]
  @converters['char'] = @converters['java.lang.Character'] = @converters[:Character] =
    @converters[SCharacter] = @converters[:Char] = @converters[:char]
  @converters['short'] = @converters['java.lang.Short'] = @converters[:Short] =
    @converters[SShort] = @converters[:short]
  @converters['int'] = @converters['java.lang.Integer'] = @converters[:Integer] =
    @converters[SInteger] = @converters[:Int] = @converters[:int]
  @converters['long'] = @converters['java.lang.Long'] = @converters[:Long] =
    @converters[SLong] = @converters[:long]
  @converters['float'] = @converters['java.lang.Float'] = @converters[:Float] =
    @converters[SFloat] = @converters[:float]
  @converters['double'] = @converters['java.lang.Double'] = @converters[:Double] =
    @converters[SDouble] = @converters[:double]
  @converters['java.math.BigDecimal'] = @converters[:BigDecimal] = @converters[:big_decimal]
    @converters[SBigDecimal] = @converters[:decimal]
  @converters['java.math.BigInteger'] = @converters[:BigInteger] = @converters[:big_integer]
    @converters[SBigInteger] = @converters[:big_int]
  @converters['java.lang.Object'] = @converters[:Object] =
    @converters[SObject] = @converters[:object]
  @converters['java.lang.String'] = @converters[:String] =
    @converters[SString] = @converters[:string]

  @default_converter = Proc.new {|val| Java.ruby_to_java(val) }
  
  @class_index = {
    :boolean => Jboolean,
    :byte => Jbyte,
    :char => Jchar,
    :short => Jshort,
    :int => Jint,
    :long => Jlong,
    :float => Jfloat,
    :double => Jdouble,
    :Boolean => JBoolean,
    SBoolean => JBoolean,
    :Byte => JByte,
    SByte => JByte,
    :Char => JCharacter,
    :Character => JCharacter,
    SCharacter => JCharacter,
    :Short => JShort,
    SShort => JShort,
    :Int => JInteger,
    :Integer => JInteger,
    SInteger => JInteger,
    :Long => JLong,
    SLong => JLong,
    :Float => JFloat,
    SFloat => JFloat,
    :Double => JDouble,
    SDouble => JDouble,
    :object => JObject,
    :Object => JObject,
    SObject => JObject,
    :string => JString,
    :String => JString,
    SString => JString,
    :decimal => JBigDecimal,
    :big_decimal => JBigDecimal,
    :BigDecimal => JBigDecimal,
    SBigDecimal => JBigDecimal,
    :big_int => JBigInteger,
    :big_integer => JBigInteger,
    :BigInteger => JBigInteger,
    SBigInteger => JBigInteger,
  }
 class << self
  def get_converter(component_type)
    converter = @converters[component_type.name]
    converter ? converter : @default_converter
  end
  
  def convert_to_type(component_type,value)
    get_converter(component_type).call(value)
  end

  # this can be expensive, as it must examine every element
  # of every 'dimension' of the Ruby array.  thinking about
  # moving this to Java.
  def dimensions(ruby_array,dims = [],index = 0)
    return [] unless ruby_array.kind_of?(::Array)
    dims << 0 while dims.length <= index 
    dims[index] = ruby_array.length if ruby_array.length > dims[index]
    ruby_array.each do |sub_array|
      next unless sub_array.kind_of?(::Array)
      dims = dimensions(sub_array,dims,index+1)
    end
    dims
  end
  
  def enable_extended_array_support
    return if [].respond_to?(:to_java)
    Array.module_eval {
      def to_java(*args,&block)
        JavaArrayUtilities.ruby_to_java(*(args.unshift(self)),&block)
      end
    }
  end

  def disable_extended_array_support
    return unless [].respond_to?(:to_java)
    Array.send(:remove_method,:to_java)
  end
 
  def fill_array(array,dimensions,fill_value)
    dims = dimensions.kind_of?(Array) ? dimensions : [dimensions]
    copy_ruby_to_java(dims,nil,array,nil,fill_value)
  end
  
private
  def get_class(class_name)
    ref =  @class_index[class_name.to_sym]
    ref ? ref.proxy : JavaUtilities.get_proxy_class(class_name.to_s)
  end
public
  
  def ruby_to_java(*args,&block)
    return JObject.proxy[].new(0) if args.length == 0
    ruby_array = args[0]
    unless ruby_array.kind_of?(::Array) || ruby_array.nil?
      raise ArgumentError,"invalid arg[0] passed to to_java (#{args[0]})"    
    end
    dims = nil
    fill_value = nil
    index = 1
    if index < args.length
      arg = args[index]
      # the (optional) first arg is class/name. if omitted,
      # defaults to java.lang.Object
      if arg.kind_of?(Class) && arg.respond_to?(:java_class)
        cls = arg
        cls_name = arg.java_class.name
        index += 1
      elsif arg.kind_of?(String) || arg.kind_of?(Symbol)
        cls = get_class(arg)
        unless cls
          raise ArgumentError,"invalid class name (#{arg}) specified for to_java"      
        end
        cls_name = arg
        index += 1
      else
        cls = JObject.proxy
        cls_name = SObject
      end
    else
      cls = JObject.proxy
      cls_name = SObject
    end
    if block
      converter = block
    elsif converter = @converters[cls_name]
    else
      converter = @default_converter
    end
    # the (optional) next arg(s) is dimensions. may be
    # specified as dim1,dim2,...,dimn, or [dim1,dim2,...,dimn]
    # the array version is required if you want to pass a
    # fill value after it
    if index < args.length
      arg = args[index]
      if arg.kind_of?(Fixnum)
        dims = [arg]
        index += 1
        while index < args.length && args[index].kind_of?(Fixnum)
          dims << args[index]
          index += 1        
        end
      elsif arg.kind_of?(::Array)
        dims = arg
        index += 1
        fill_value = converter.call(args[index]) if index < args.length
      elsif arg.nil?
        dims = dimensions(ruby_array) if ruby_array
        index += 1
        fill_value = converter.call(args[index]) if index < args.length
      end
    else
      dims = dimensions(ruby_array) if ruby_array
    end
    dims = [0] unless dims
    java_array = ArrayJavaProxyCreator.new(cls.java_class,*dims).new
    if ruby_array
      copy_ruby_to_java(dims,ruby_array,java_array,converter,fill_value)          
    elsif fill_value
      copy_ruby_to_java(dims,nil,java_array,converter,fill_value)
    end
    java_array
  end

private
  def copy_ruby_to_java(dims,ruby_array,java_array,converter,fill_value)
    if dims.length > 1
      shift_dims = dims[1...dims.length]
      for i in 0...dims[0]
        if ruby_array.kind_of?(::Array)
          ruby_param = ruby_array[i]
        else
          ruby_param = ruby_array # fill with value when no array        
        end
        copy_ruby_to_java(shift_dims,ruby_param,java_array[i],converter,fill_value)
      end
    else
      copy_data(ruby_array,java_array,converter,fill_value)
    end
    java_array 
  end
  
private
  def copy_data(ruby_array,java_array,converter,fill_value)
    if ruby_array.kind_of?(::Array)
      rlen = ruby_array.length
    else
      rlen = 0
      # in irregularly-formed Ruby arrays, values that appear where
      # a subarray is expected get propagated. not sure if this is
      # the best behavior, will see what users say
      fill_value = converter.call(ruby_array) if ruby_array    
    end
    java_object = java_array.java_object
    jlen = java_array.length
    i = 0
    while i < rlen && i < jlen
      java_object[i] = converter.call(ruby_array[i])
      i += 1
    end
    if i < jlen && fill_value
      java_object.fill(i,jlen,fill_value)
    end
    java_array
  end
public

  def java_to_ruby(java_array)
    unless java_array.kind_of?(ArrayJavaProxy)
      raise ArgumentError,"not a Java array: #{java_array}"
    end
    length = java_array.length
    ruby_array = Array.new(length)
    if length > 0
      if java_array[0].kind_of?ArrayJavaProxy
        length.times do |i|
          ruby_array[i] = java_to_ruby(java_array[i])      
        end
      else
        length.times do |i|
          ruby_array[i] = java_array[i];      
        end
      end
    end
    ruby_array
  end

 end #self
end #JavaArrayUtilities

# enable :to_java for arrays
class Array
  def to_java(*args,&block)
    JavaArrayUtilities.ruby_to_java(*(args.unshift(self)),&block)
  end
end

# Extensions to the standard Module package.

class Module
  private

  ##
  # Includes a Java package into this class/module. The Java classes in the
  # package will become available in this class/module, unless a constant
  # with the same name as a Java class is already defined.
  #
  def include_package(package_name)
    if defined? @included_packages
      @included_packages << package_name      
      return
    end
    @included_packages = [package_name]
    @java_aliases = {} unless @java_aliases

    def self.const_missing(constant)
      real_name = @java_aliases[constant]
      real_name = constant unless real_name

      java_class = nil
      return super unless @included_packages.detect {|package|
          java_class = JavaUtilities.get_java_class(package + '.' + real_name.to_s)
      }
      
      JavaUtilities.create_proxy_class(constant, java_class, self)
    end
  end

  def java_alias(new_id, old_id)
    @java_aliases[new_id] = old_id
  end
end

class ConstantAlreadyExistsError < RuntimeError
end

class Object
  def include_class(include_class)
    class_names = include_class.to_a

    class_names.each do |full_class_name|
      package_name, class_name = full_class_name.match(/((.*)\.)?([^\.]*)/)[2,3]

      if block_given?
        constant = yield(package_name, class_name)
      else
        constant = class_name
      end
      
      cls = self.kind_of?(Module) ? self : self.class

	  # Constant already exists...do not let proxy get created unless the collision is the proxy
	  # you are trying to include.
      if (cls.const_defined?(constant) )
        proxy = JavaUtilities.get_proxy_class(full_class_name)
        existing_constant = cls.const_get(constant)
      	raise ConstantAlreadyExistsError.new, "Class #{constant} already exists" unless existing_constant == proxy
	  end

      # FIXME: When I changed this user const_set instead of eval below Comparator got lost
      # which means I am missing something.
      if (respond_to?(:class_eval, true))
        class_eval("#{constant} = JavaUtilities.get_proxy_class(\"#{full_class_name}\")")
      else
        eval("#{constant} = JavaUtilities.get_proxy_class(\"#{full_class_name}\")")
      end
    end
  end

  # sneaking this in with the array support, getting
  # tired of having to define it in all my code  -BD
  def java_kind_of?(other)
    return true if self.kind_of?(other)
    return false unless self.respond_to?(:java_class) && other.respond_to?(:java_class) &&
      other.kind_of?(Module) && !self.kind_of?(Module) 
    return other.java_class.assignable_from?(self.java_class)
  end
end

class JavaInterfaceExtender
  def initialize(java_class_name, &block)
    @java_class = Java::JavaClass.for_name(java_class_name)
    @block = block
  end
  
  def extend_proxy(proxy_class)
    proxy_class.class_eval &@block if @java_class.assignable_from? proxy_class.java_class
  end
end

module Java
 class << self
   def const_missing(sym)
      JavaUtilities.get_proxy_class "#{sym}"
   end

   def method_missing(sym, *args)
     if JavaUtilities.is_primitive_type(sym) 
       JavaUtilities.get_proxy_class sym.to_s
     elsif sym.to_s.downcase[0] == sym.to_s[0]
       Package.create_package sym, sym, Java
     else
       JavaUtilities.get_proxy_class "#{sym}"
     end
   end
 end

 class Package
   # this class should be a blank slate
   
   def initialize(name)
     @name = name
   end

   def singleton; class << self; self; end; end 

   def method_missing(sym, *args)
     if sym.to_s.downcase[0] == sym.to_s[0]
       self.class.create_package sym, "#{@name}.#{sym}", singleton
     else
       JavaUtilities.get_proxy_class "#{@name}.#{sym}"
     end
   end

   class << self
     def create_package(sym, package_name, cls)
       package = Java::Package.new package_name
       cls.send(:define_method, sym) { package }
       package
     end
   end
 end
end

# Create convenience methods for top-level java packages so we do not need to prefix
# with 'Java::'.  We undef these methods within Package in case we run into 'com.foo.com'.
[:java, :javax, :com, :org].each do |meth|
 Java::Package.create_package(meth, meth.to_s, Kernel)
 Java::Package.send(:undef_method, meth)
end

require 'builtin/java/exceptions'
require 'builtin/java/collections'
require 'builtin/java/interfaces'
