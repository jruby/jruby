require 'ffi'

module JRuby
  module FFI
    class BaseStruct
      Buffer = JRuby::FFI::Buffer
      attr_reader :pointer
  
      def initialize(pointer = nil, *spec)
        @cspec = self.class.layout(*spec)

        if pointer then
          @pointer = pointer
        else
          @pointer = MemoryPointer.new size
        end
      end
      def self.alloc_inout(clear = true)
        self.new(Buffer.__alloc_inout(@size, clear))
      end
      def self.alloc_in(clear = true)
        self.new(Buffer.__alloc_in(@size, clear))
      end
      def self.alloc_out(clear = true)
        self.new(Buffer.__alloc_out(@size, clear))
      end
      def self.size
        @size
      end
      def self.members
        @layout.members
      end
      def size
        self.class.size
      end
      def [](field)
        @cspec.get(@pointer, field)
      end
      def []=(field, val)
        @cspec.put(@pointer, field, val)
      end
      def members
        @cspec.members
      end
      def values
        @cspec.members.map { |m| self[m] }
      end
      def clear
        @pointer.clear
        self
      end
    end
    class Struct < JRuby::FFI::BaseStruct
  
      def self.layout(*spec)
    
        return @layout if spec.size == 0
        spec = spec[0]
        builder = JRuby::FFI::StructLayoutBuilder.new
        spec.each do |name,type|
          builder.add_field(name, JRuby::FFI.find_type(type))
        end
        cspec = builder.build
        @layout = cspec unless self == JRuby::FFI::Struct
        @size = cspec.size
        return cspec
      end
    end
  end
end


module FFI
  class Struct < JRuby::FFI::BaseStruct
    def self.layout(*spec)
      return @layout if spec.size == 0

      builder = JRuby::FFI::StructLayoutBuilder.new
      i = 0
      while i < spec.size
        name, type, offset = spec[i, 3]
      
        code = FFI.find_type(type)
        builder.add_field(name, code, offset)
        i += 3
      end
      cspec = builder.build
      @layout = cspec unless self == FFI::Struct
      @size = cspec.size
      return cspec
    end

    def self.config(base, *fields)
      config = JRuby::FFI::Config::CONFIG
      @size = config["#{base}.sizeof"]
    
      builder = JRuby::FFI::StructLayoutBuilder.new
    
      fields.each do |field|
        offset = config["#{base}.#{field}.offset"]
        size   = config["#{base}.#{field}.size"]
        type   = config["#{base}.#{field}.type"]
        type   = type ? type.to_sym : JRuby::FFI.size_to_type(size)

        code = FFI.find_type type
        if (code == NativeType::CHAR_ARRAY)
          builder.add_char_array(field.to_s, size, offset)
        else
          builder.add_field(field.to_s, code, offset)
        end
      end
      cspec = builder.build
    
      @layout = cspec
      @size = cspec.size if @size < cspec.size
    
      return cspec
    end
  end
end
