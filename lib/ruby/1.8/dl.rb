require 'java'

warn "DL: This is only a partial implementation, and it's likely broken"

module DL
  class DLError < StandardError; end
  class DLTypeError < StandardError; end
  
  FuncTable = {}
  
  RTLD_GLOBAL = 0
  RTLD_LAZY = 0
  RTLD_NOW = 0
  
  ALIGN_INT = 0
  ALIGN_LONG = 0
  ALIGN_FLOAT = 0
  ALIGN_SHORT = 0
  ALIGN_DOUBLE = 0
  ALIGN_VOIDP = 0
  
  MAX_ARG = 0
  DLSTACK = 0
  
  FREE = 
  
  def self.dlopen(*args)
    Handle.new(*args)
  end
  
  def self.callback
    
  end
  
  def self.define_callback
    
  end
  
  def self.remove_callback
    
  end
  
  def self.malloc(*args)
    PtrData.malloc(*args)
  end
  
  def self.strdup
    
  end
  
  def self.sizeof
    
  end
  
  class ::String
    def to_ptr
      
    end
  end
  
  class ::Array
    def to_ptr
      
    end
  end
  
  class ::IO
    def to_ptr
      
    end
  end
  
  class Handle
    import com.sun.jna.NativeLibrary
    
    def initialize(clib, cflag = 0)
      @ptr = NativeLibrary.get_instance(clib)
      @open = true
      @enable_close = false
      
      if block_given?
        begin
          yield self
        ensure
          # TODO: close somehow?
          @open = false
        end
      end
      
      return nil
    rescue
      raise $!.to_s
    end
    
    def to_i
      
    end
    
    def to_ptr
      
    end
    
    def close
      
    end
    
    def sym(name, type = nil)
      unless @open
        raise "closed handle"
      end
      
      begin
        func = @ptr.get_function(name)
        raise "unknown symbol \"#{name}\"" unless func
        
        return Symbol.new(func, name, type)
      end
    end
    
    def []
      
    end
    
    def disable_close
      @enable_close = false
    end
    
    def enable_close
      @enable_close = true
    end
  end
  
  class PtrData
    def self.malloc; end
    def initialize; end
    def free=; end
    def free; end
    def to_i; end
    def ptr; end
    def +@; end
    def ref; end
    def -@; end
    def null?; end
    def to_a; end
    def to_s; end
    def to_str; end
    def inspect; end
    def <=>; end
    def ==; end
    def eql?; end
    def +; end
    def -; end
    def define_data_type; end
    def struct!; end
    def union!; end
    def []; end
    def []=; end
    def size; end
    def size=; end
    
    module MemorySpace
      MemoryTable = {}
      
      def self.each; end
    end
  end
  
  class Symbol
    def self.char2type; end
    
    def initialize(func, name, type)
      @func = func
      @name = name.dup
      @type = type.dup
      
      nil
    end
    
    def call(*args)
      types = []
      @type.each_byte do |b|
        case b
        when ?p
        when ?P
        when ?a
        when ?A
        when ?C, ?c
          types << java.lang.Byte
        when ?H
        when ?h
        when ?I, ?i
          types << java.lang.Integer
        when ?L, ?l
          types << java.lang.Long
        when ?F, ?f
          types << java.lang.Float
        when ?D,?d
          types << java.lang.Double
        when ?S,?s
          types << java.lang.String
        else raise "unknown type '#{b.chr}'"
        end
      end
      
      ret_type = types.shift
      function = get_function_from_return(ret_type)
      
      types = types.to_java java.lang.Class
      
      values = []
      
      args && args.each_index do |i|
        # TODO: how to do more reliable type coercion here based on types array?
        values << Java.ruby_to_java(args[i])
      end
      
      values = values.to_java :object
      
      [@func.send(function, values), args]
    end
    
    alias [] call
    
    def name; @name; end
    def proto; @type; end
    def cproto; end
    def inspect; end
    def to_s; end
    def to_ptr; end
    def to_i; end
    
    private
    
    def get_function_from_return(ret_type)
      if ret_type == java.lang.Integer
        return :invokeInt
      elsif ret_type == java.lang.String
        return :invokeString
      elsif ret_type == java.lang.Long
        return :invokeLong
      elsif ret_type == java.lang.Float
        return :invokeFloat
      elsif ret_type == java.lang.Double
        return :invokeDouble
      end
    end
  end
  
  def self.last_error; end
  def self.last_error=; end
  
  def self.win32_last_error; end
  def self.win32_last_error=; end
end