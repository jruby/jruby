# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

def plus_int(a, b)
  a + b
end

Truffle::Interop.export_method(:plus_int)

def fourty_two
  42
end

Truffle::Interop.export_method(:fourty_two)

def ret_nil
  nil
end

Truffle::Interop.export_method(:ret_nil)

$invocations = 0

def count_invocations
  $invocations += 1
end

Truffle::Interop.export_method(:count_invocations)

def apply_numbers(f)
  f.call(18, 32) + 10
end

Truffle::Interop.export_method(:apply_numbers)

def compound_object
  obj = Object.new

  def obj.fourtyTwo
    42
  end

  def obj.plus(a, b)
    a + b
  end

  def obj.returnsNull
    nil
  end

  def obj.returnsThis
    self
  end

  obj
end

Truffle::Interop.export_method(:compound_object)

def identity(value)
  value
end

Truffle::Interop.export_method(:identity)

def evaluate_source(mime, source)
  # TODO CS-21-Dec-15 java_string_to_ruby shouldn't be needed - we need to convert j.l.String to Ruby's String automatically

  Truffle::Interop.eval(
      Truffle::Interop.unbox(mime),
      Truffle::Interop.unbox(source))
end

Truffle::Interop.export_method(:evaluate_source)

def complex_add(a, b)
  a.imaginary = a.imaginary + b.imaginary
  a.real = a.real + b.real
end

Truffle::Interop.export_method(:complex_add)

def complex_add_with_method(a, b)
  a.imaginary = a.imaginary + b.imaginary
  a.real = a.real + b.real
end

Truffle::Interop.export_method(:complex_add_with_method)

def complex_sum_real(complexes)
  complexes = Truffle::Interop.enumerable(complexes)

  complexes.map{ |c| c.real }.inject(&:+)
end

Truffle::Interop.export_method(:complex_sum_real)

def complex_copy(a, b)
  a = Truffle::Interop.enumerable(a)
  b = Truffle::Interop.enumerable(b)

  # TODO CS 21-Dec-15
  # If we don't force b to an array here, the zip below will try to iterate both a and b at the same time. It can't do
  # that with Ruby blocks, so it creates a Fiber (a Java thread) to do it using two separate call stacks. That causes
  # com.oracle.truffle.api.interop.ForeignAccess.checkThread(ForeignAccess.java:133) to fail. What do we do about this?
  b = b.to_a

  a.zip(b).each do |x, y|
    x.imaginary = y.imaginary
    x.real = y.real
  end
end

Truffle::Interop.export_method(:complex_copy)

class ValuesClass

  attr_accessor :byteValue
  attr_accessor :shortValue
  attr_accessor :intValue
  attr_accessor :longValue
  attr_accessor :floatValue
  attr_accessor :doubleValue
  attr_accessor :charValue
  attr_accessor :stringValue
  attr_accessor :booleanValue

  def initialize
    @byteValue = 0
    @shortValue = 0
    @intValue = 0
    @longValue = 0
    @floatValue = 0.0
    @doubleValue = 0.0
    @charValue = '0'
    @stringValue = ''
    @booleanValue = false
  end

end

def values_object
  ValuesClass.new
end

Truffle::Interop.export_method(:values_object)

def add_array(array, index, value)
  array[index] += value
end

Truffle::Interop.export_method(:add_array)

def count_up_while(f)
  counter = 0
  loop do
    break unless f.call(counter)
    counter += 1
  end
end

Truffle::Interop.export_method(:count_up_while)

def object_with_element
  [1, 2, 42.0, 4]
end

Truffle::Interop.export_method(:object_with_element)

class ObjectWithValueProperty

  attr_accessor :value

  def initialize
    @value = 42.0
  end

end

def object_with_value_property
  ObjectWithValueProperty.new
end

Truffle::Interop.export_method(:object_with_value_property)

def function_add_numbers
  proc do |a, b|
    a + b
  end
end

Truffle::Interop.export_method(:function_add_numbers)

class ObjectWithValueAndAddProperty

  attr_accessor :value

  def initialize
    @value = 42.0
  end

  def add(other)
    @value + other
  end

end

def object_with_value_and_add_property
  ObjectWithValueAndAddProperty.new
end

Truffle::Interop.export_method(:object_with_value_and_add_property)

def call_function(function)
  function.call 41.0, 42.0
end

Truffle::Interop.export_method(:call_function)

def call_method(object)
  object.foo 41.0, 42.0
end

Truffle::Interop.export_method(:call_method)

def read_value_from_foreign(object)
  object.value
end

Truffle::Interop.export_method(:read_value_from_foreign)

def read_element_from_foreign(object)
  object[2]
end

Truffle::Interop.export_method(:read_element_from_foreign)

def write_value_to_foreign(object)
  object.value = 42.0
end

Truffle::Interop.export_method(:write_value_to_foreign)

def write_element_to_foreign(object)
  object[2] = 42.0
end

Truffle::Interop.export_method(:write_element_to_foreign)

def get_size_of_foreign(object)
  Truffle::Interop.size(object)
end

Truffle::Interop.export_method(:get_size_of_foreign)

def has_size_of_foreign(object)
  Truffle::Interop.size?(object)
end

Truffle::Interop.export_method(:has_size_of_foreign)

def is_null_foreign(object)
  object.nil?
end

Truffle::Interop.export_method(:is_null_foreign)

def is_executable_of_foreign(object)
  Truffle::Interop.executable?(object)
end

Truffle::Interop.export_method(:is_executable_of_foreign)
