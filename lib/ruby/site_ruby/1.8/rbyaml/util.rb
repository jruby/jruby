class Object
  def __is_str; false end
  def __is_sym; false end
  def __is_a; false end
  def __is_int; false end
end

class String
  def __is_str; true end
end

class Symbol
  def __is_sym; true end
end

class Array
  def __is_a; true end
end

class Integer
  def __is_int; true end
end

class Fixnum
  def __is_ascii_num
    self <= ?9 && self >= ?0
  end
end

module RbYAML
  class PrivateType
    attr_accessor :type_id, :value
    def initialize( type, val )
      @type_id = type; 
      @value = val
    end
  end
end
