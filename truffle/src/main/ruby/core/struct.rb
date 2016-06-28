# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Struct
  include Enumerable

  class << self
    alias_method :subclass_new, :new
  end

  def self.new(klass_name, *attrs, &block)
    if klass_name
      if klass_name.kind_of? Symbol # Truffle: added to avoid exception and match MRI
        attrs.unshift klass_name
        klass_name = nil
      else
        begin
          klass_name = StringValue klass_name
        rescue TypeError
          attrs.unshift klass_name
          klass_name = nil
        end
      end
    end

    attrs = attrs.map do |a|
      case a
      when Symbol
        a
      when String
        sym = a.to_sym
        unless sym.kind_of? Symbol
          raise TypeError, "#to_sym didn't return a symbol"
        end
        sym
      else
        raise TypeError, "#{a.inspect} is not a symbol"
      end
    end

    klass = Class.new self do
      _specialize attrs
      attr_accessor(*attrs)

      def self.new(*args, &block)
        return subclass_new(*args, &block)
      end

      def self.[](*args)
        return new(*args)
      end

      const_set :STRUCT_ATTRS, attrs
    end

    Struct.const_set klass_name, klass if klass_name

    klass.module_eval(&block) if block

    return klass
  end

  def self.make_struct(name, attrs)
    new name, *attrs
  end

  def _attrs # :nodoc:
    return self.class::STRUCT_ATTRS
  end
  private :_attrs

  def select
    return to_enum(:select) { size } unless block_given?

    to_a.select do |v|
      yield v
    end
  end

  def to_h
    Hash[each_pair.to_a]
  end

  def to_s
    return "[...]" if Thread.guarding? self

    Thread.recursion_guard self do
      values = []

      _attrs.each do |var|
        val = instance_variable_get :"@#{var}"
        values << "#{var}=#{val.inspect}"
      end

      name = self.class.name

      if name.nil? || name.empty?
        "#<struct #{values.join(', ')}>"
      else
        "#<struct #{self.class.name} #{values.join(', ')}>"
      end
    end
  end

  alias_method :inspect, :to_s

  def instance_variables
    # Hide the ivars used to store the struct fields
    super() - _attrs.map { |a| "@#{a}".to_sym }
  end

  def initialize(*args)
    attrs = _attrs

    unless args.length <= attrs.length
      raise ArgumentError, "Expected #{attrs.size}, got #{args.size}"
    end

    attrs.each_with_index do |attr, i|
      instance_variable_set :"@#{attr}", args[i]
    end
  end

  private :initialize

  def ==(other)
    return false if self.class != other.class

    Thread.detect_recursion self, other do
      return self.values == other.values
    end

    # Subtle: if we are here, we are recursing and haven't found any difference, so:
    true
  end

  def [](var)
    case var
    when Symbol, String
      # ok
    else
      var = Integer(var)
      a_len = _attrs.length
      if var > a_len - 1
        raise IndexError, "offset #{var} too large for struct(size:#{a_len})"
      end
      if var < -a_len
        raise IndexError, "offset #{var + a_len} too small for struct(size:#{a_len})"
      end
      var = _attrs[var]
    end

    unless _attrs.include? var.to_sym
      raise NameError, "no member '#{var}' in struct"
    end

    return instance_variable_get(:"@#{var}")
  end

  def []=(var, obj)
    case var
    when Symbol
      unless _attrs.include? var
        raise NameError, "no member '#{var}' in struct"
      end
    when String
      var = var.to_sym
      unless _attrs.include? var
        raise NameError, "no member '#{var}' in struct"
      end
    else
      var = Integer(var)
      a_len = _attrs.length
      if var > a_len - 1
        raise IndexError, "offset #{var} too large for struct(size:#{a_len})"
      end
      if var < -a_len
        raise IndexError, "offset #{var + a_len} too small for struct(size:#{a_len})"
      end

      var = _attrs[var]
    end

    return instance_variable_set(:"@#{var}", obj)
  end

  def dig(key, *more)
    result = nil
    begin
      result = self[key]
    rescue IndexError, NameError
    end
    if result.nil? || more.empty?
      result
    else
      raise TypeError, "#{result.class} does not have #dig method" unless result.respond_to?(:dig)
      result.dig(*more)
    end
  end

  def eql?(other)
    return true if equal? other
    return false if self.class != other.class

    Thread.detect_recursion self, other do
      _attrs.each do |var|
        mine =   instance_variable_get(:"@#{var}")
        theirs = other.instance_variable_get(:"@#{var}")

        return false unless mine.eql? theirs
      end
    end

    # Subtle: if we are here, then no difference was found, or we are recursing
    # In either case, return
    true
  end

  def each
    return to_enum(:each) { size } unless block_given?
    values.each do |v|
      yield v
    end
    self
  end

  def each_pair
    return to_enum(:each_pair) { size } unless block_given?
    _attrs.each { |var| yield [var, instance_variable_get(:"@#{var}")] }
    self
  end

  def hash
    hash_val = size
    return _attrs.size if Thread.detect_outermost_recursion self do
      _attrs.each { |var| hash_val ^= instance_variable_get(:"@#{var}").hash }
    end
    return hash_val
  end

  def length
    return _attrs.length
  end

  alias_method :size, :length

  def self.length
    return self::STRUCT_ATTRS.size
  end

  def self.members
    self::STRUCT_ATTRS.dup
  end

  def members
    return self.class.members
  end

  def to_a
    return _attrs.map { |var| instance_variable_get :"@#{var}" }
  end

  alias_method :values, :to_a

  def values_at(*args)
    to_a.values_at(*args)
  end

  def self._specialize(attrs)
    # Because people are crazy, they subclass Struct directly, ie.
    #  class Craptastic < Struct
    #
    # NOT
    #
    #  class Fine < Struct.new(:x, :y)
    #
    # When they do this craptastic act, they'll sometimes define their
    # own #initialize and super into Struct#initialize.
    #
    # When they do this and then do Craptastic.new(:x, :y), this code
    # will accidentally shadow their #initialize. So for now, only run
    # the specialize if we're trying new Struct's directly from Struct itself,
    # not a craptastic Struct subclass.

    return unless superclass.equal? Struct

    # To allow for optimization, we generate code with normal ivar
    # references for all attributes whose names can be written as
    # tIVAR tokens. For example, of the following struct attributes
    #
    #   Struct.new(:a, :@b, :c?, :'d-e')
    #
    # only the first, :a, can be written as a valid tIVAR token:
    #
    #   * :a can be written as @a
    #   * :@b becomes @@b and would be interpreted as a tCVAR
    #   * :c? becomes @c? and be interpreted as the beginning of
    #     a ternary expression
    #   * :'d-e' becomes @d-e and would be interpreted as a method
    #     invocation
    #
    # Attribute names that cannot be written as tIVAR tokens will
    # fall back to using #instance_variable_(get|set).

    args, assigns, hashes, vars = [], [], [], []

    attrs.each_with_index do |name, i|
      name = "@#{name}"

      if name =~ /^@[a-z_]\w*$/i
        assigns << "#{name} = a#{i}"
        vars    << name
      else
        assigns << "instance_variable_set(:#{name.inspect}, a#{i})"
        vars    << "instance_variable_get(:#{name.inspect})"
      end

      args   << "a#{i} = nil"
      hashes << "#{vars[-1]}.hash"
    end

    code = <<-CODE
      def initialize(#{args.join(", ")})
        #{assigns.join(';')}
        self
      end

      def hash
        hash = #{hashes.size}

        return hash if Thread.detect_outermost_recursion(self) do
          hash = hash ^ #{hashes.join(' ^ ')}
        end

        hash
      end

      def to_a
        [#{vars.join(', ')}]
      end

      def length
        #{vars.size}
      end
    CODE

    begin
      mod = Module.new do
        module_eval code
      end
      include mod
    rescue SyntaxError
      # SyntaxError means that something is wrong with the
      # specialization code. Just eat the error and don't specialize.
    end
  end
end
