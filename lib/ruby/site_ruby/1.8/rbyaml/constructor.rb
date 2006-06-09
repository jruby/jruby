require 'base64'
require 'set'

require 'rbyaml/util'

require 'rbyaml/error'
require 'rbyaml/nodes'
require 'rbyaml/composer'

class Symbol
  def __call(obj,*args)
    obj.send(self,*args)
  end
end

class Proc
  def __call(obj,*args)
    call(obj,*args)
  end
end

class Method
  def __call(obj,*args)
    call(*args)
  end
end


module RbYAML
  class ConstructorError < MarkedYAMLError
  end
  
  class BaseConstructor
    @@yaml_constructors = {}
    @@yaml_multi_constructors = {}
    @@yaml_multi_regexps = {}
    
    def initialize(composer)
      @composer = composer
      @constructed_objects = {}
      @recursive_objects = {}
    end

    def check_data
      # If there are more documents available?
      @composer.check_node
    end

    def get_data
      # Construct and return the next document.
      construct_document(@composer.get_node) if @composer.check_node
    end
    
    def each_document
      # Iterator protocol.
      while @composer.check_node
        yield construct_document(@composer.get_node)
      end
    end 

    def construct_document(node)
      data = construct_object(node)
      @constructed_objects = {}
      @recursive_objects = {}
      data
    end

    class RecursiveProxy
      attr_writer :value
      def method_missing(*args)
        @value.send(*args)
      end
      def class
        @value.class
      end
      def to_s
        @value.to_s
      end
    end
    
    def construct_object(node)
      return @constructed_objects[node] if @constructed_objects.include?(node)
      @constructed_objects[node] = RecursiveProxy.new
      constructor = @@yaml_constructors[node.tag]
      if !constructor
        ruby_cls = RbYAML::tagged_classes[node.tag]
        if ruby_cls && (ruby_cls.method_defined?(:yaml_initialize) || ruby_cls.respond_to?(:yaml_new))
          constructor = lambda { |obj,node| send(:construct_ruby_object,ruby_cls,node) }
        else
          through = true
          for tag_prefix,reg in @@yaml_multi_regexps
            if reg =~ node.tag
              tag_suffix = node.tag[tag_prefix.length..-1]
              constructor = lambda { |obj, node| @@yaml_multi_constructors[tag_prefix].__call(self,tag_suffix, node) }
              through = false
              break
            end
          end
          if through
            ctor = @@yaml_multi_constructors[nil]
            if ctor
              constructor = lambda { |obj, node| ctor.__call(self,node.tag,node) }
            else
              ctor = @@yaml_constructors[nil]
              if ctor
                constructor = lambda { |obj, node| ctor.__call(self,node)}
              else
                constructor = lambda { |obj, node| construct_primitive(node) }
              end
            end
          end
        end
      end
      data = constructor.__call(self,node)
      @constructed_objects[node].value = data
      @constructed_objects[node] = data
      data
    end

    def construct_primitive(node)
      if node.__is_scalar
        construct_scalar(node)
      elsif node.__is_sequence
        construct_sequence(node)
      elsif node.__is_mapping
        construct_mapping(node)
      else
        puts node.tag
      end
    end
    
    def construct_scalar(node)
      if !node.__is_scalar
        if node.__is_mapping
          for key_node in node.value.keys
            if key_node.tag == "tag:yaml.org,2002:value"
              return construct_scalar(node.value[key_node])
            end
          end
        end
        raise ConstructorError.new(nil, nil,"expected a scalar node, but found #{node.tid}",node.start_mark)
      end
      node.value
    end

    def construct_private_type(node)
#      construct_scalar(node)
      PrivateType.new(node.tag,node.value)
    end

    def construct_sequence(node)
      raise ConstructorError.new(nil,nil,"expected a sequence node, but found #{node.tid}",node.start_mark) if !node.__is_sequence
      node.value.map {|child| construct_object(child) }
    end

    def construct_mapping(node)
      raise ConstructorError.new(nil,nil,"expected a mapping node, but found #{node.tid}",node.start_mark) if !node.__is_mapping
      mapping = {}
      merge = nil
      for key_node,value_node in node.value
        if key_node.tag == "tag:yaml.org,2002:merge"
          raise ConstructorError.new("while constructing a mapping", node.start_mark,"found duplicate merge key", key_node.start_mark) if !merge.nil?
          if value_node.__is_mapping
            merge = [construct_mapping(value_node)]
          elsif value_node.__is_sequence
            merge = []
            for subnode in value_node.value
              if !subnode.__is_mapping
                raise ConstructorError.new("while constructing a mapping",node.start_mark,"expected a mapping for merging, but found #{subnode.tid}", subnode.start_mark)
              end
              merge.unshift(construct_mapping(subnode))
            end
          else
            raise ConstructorError.new("while constructing a mapping", node.start_mark,"expected a mapping or list of mappings for merging, but found #{value_node.tid}", value_node.start_mark)
          end
        elsif key_node.tag == "tag:yaml.org,2002:value"
          raise ConstructorError.new("while construction a mapping", node.start_mark,"found duplicate value key", key_node.start_mark) if mapping.include?("=")
          value = construct_object(value_node)
          mapping["="] = value
        else
          key = construct_object(key_node)
          value = construct_object(value_node)
          mapping[key] = value
#          raise ConstructorError.new("while constructing a mapping", node.start_mark,"found duplicate key", key_node.start_mark) if mapping.include?(key)
        end
      end
      if !merge.nil?
        merge << mapping
        mapping = { }
        for submapping in merge
          mapping.merge!(submapping)
        end
      end
      mapping
    end

    def construct_pairs(node)
      raise ConstructorError.new(nil,nil,"expected a mapping node, but found #{node.tid}",node.start_mark) if !node.__is_mapping
      node.value.collect {|key_node,value_node| [construct_object(key_node), construct_object(value_node)] }
    end

    def self.add_constructor(tag, constructor)
      @@yaml_constructors[tag] = constructor
    end

    def self.add_multi_constructor(tag_prefix, multi_constructor)
      @@yaml_multi_constructors[tag_prefix] = multi_constructor
      @@yaml_multi_regexps[tag_prefix] = Regexp.new("^"+Regexp.escape(tag_prefix))
    end
  end

  class SafeConstructor < BaseConstructor
    def construct_yaml_null(node)
      construct_scalar(node)
      nil
    end
    
    BOOL_VALUES = {
      "y" =>       true,
      "n" =>       false,
      "yes" =>     true,
      "no" =>      false,
      "true" =>    true,
      "false" =>   false,
      "on" =>      true,
      "off" =>     false
    }

    def construct_yaml_bool(node)
      value = construct_scalar(node)
      BOOL_VALUES[value.downcase]
    end
    
    def construct_yaml_int(node)
      value = construct_scalar(node).to_s
      value = value.gsub(/_/, '')
      sign = +1
      first = value[0]
      if first == ?-
        sign = -1
        value.slice!(0)
      elsif first == ?+
        value.slice!(0)
      end
      base = 10
      if value == "0"
        return 0
      elsif value[0..1] == "0b"
        value.slice!(0..1)
        base = 2
      elsif value[0..1] == "0x"
        value.slice!(0..1)
        base = 16
      elsif value[0] == ?0
        value.slice!(0)
        base = 8
      elsif value.include?(?:)
        digits = (value.split(/:/).map {|val| val.to_i}).reverse
        base = 1
        value = 0
        for digit in digits
          value += digit*base
          base *= 60
        end
        return sign*value
      else
        return sign*value.to_i
      end
      return sign*value.to_i(base)
    end

    INF_VALUE = +1.0/0.0
    NAN_VALUE = 0.0/0.0
    
    def construct_yaml_float(node)
      value = construct_scalar(node).to_s
      value = value.gsub(/_/, '')
      sign = +1
      first = value[0]
      if first == ?-
        sign = -1
        value.slice!(0)
      elsif first == ?+
        value.slice!(0)
      end
      if value.downcase == ".inf"
        return sign*INF_VALUE
      elsif value.downcase == ".nan"
        return NAN_VALUE
      elsif value.include?(?:)
        digits = (value.split(/:/).map {|val| val.to_f}).reverse
        base = 1
        value = 0.0
        for digit in digits
          value += digit*base
          base *= 60
        end
        return sign*value
      else
        return value.to_f
      end
    end

    def construct_yaml_binary(node)
      value = construct_scalar(node)
      Base64.decode64(value.split(/[\n\x85]|(?:\r[^\n])/).to_s)
    end

    TIMESTAMP_REGEXP = /^([0-9][0-9][0-9][0-9])-([0-9][0-9]?)-([0-9][0-9]?)(?:(?:[Tt]|[ \t]+)([0-9][0-9]?):([0-9][0-9]):([0-9][0-9])(?:\.([0-9]*))?(?:[ \t]*(?:Z|([-+][0-9][0-9]?)(?::([0-9][0-9])?)?))?)?$/
    
    def construct_yaml_timestamp(node)
      unless (match = TIMESTAMP_REGEXP.match(node.value))
        return construct_private_type(node)
      end
      values = match.captures.map {|val| val.to_i}
      fraction = values[6]
      if fraction != 0
        fraction *= 10 while 10*fraction < 1000
        values[6] = fraction
      end
      stamp = Time.gm(values[0],values[1],values[2],values[3],values[4],values[5],values[6])
      
      diff = values[7] * 3600 + values[8] * 60
      return stamp-diff
    end
    
    def construct_yaml_omap(node)
      # Note: we do not check for duplicate keys, because its too
      # CPU-expensive.
      raise ConstructorError.new("while constructing an ordered map", node.start_mark,
                                 "expected a sequence, but found #{node.tid}", node.start_mark) if !node.__is_sequence
      omap = []
      for subnode in node.value
        raise ConstructorError.new("while constructing an ordered map", node.start_mark,
                                   "expected a mapping of length 1, but found #{subnode.tid}",subnode.start_mark) if !subnode.__is_mapping
        raise ConstructorError.new("while constructing an ordered map", node.start_mark,
                                   "expected a single mapping item, but found #{subnode.value.length} items",subnode.start_mark) if subnode.value.length != 1
        key_node = subnode.value.keys[0]
        key = construct_object(key_node)
        value = construct_object(subnode.value[key_node])
        omap << [key, value]
      end
      omap
    end

    def construct_yaml_pairs(node)
      construct_yaml_omap(node)
    end

    def construct_yaml_set(node)
      Set.new(construct_mapping(node).keys)
    end
    
    def construct_yaml_str(node)
      val = construct_scalar(node).to_s
      val.empty? ? nil : val
    end

    def construct_yaml_seq(node)
      construct_sequence(node)
    end
    
    def construct_yaml_map(node)
      construct_mapping(node)
    end

    def construct_yaml_object(node, cls)
      mapping = construct_mapping(node)
      data = cls.new
      mapping.each {|key,val| data.instance_variable_set("@#{key}",val)}
      data
    end

    def construct_undefined(node)
      raise ConstructorError.new(nil,nil,"could not determine a constructor for the tag #{node.tag}",node.start_mark)
    end

    def construct_ruby_object(cls,node)
      val = construct_primitive(node)
      if cls.respond_to?(:yaml_new)
        obj = cls.yaml_new(cls,node.tag,val)
      else
        obj = cls.allocate
        obj.yaml_initialize(node.tag,val)
      end
      obj
    end

    def construct_ruby(tag,node)
      obj_class = Object
      tag.split( "::" ).each { |c| obj_class = obj_class.const_get( c ) } if tag
      o = obj_class.allocate
      mapping = construct_mapping(node)
      mapping.each {|key,val| o.instance_variable_set("@#{key}",val)}
      o
    end
  end
  
  SafeConstructor::add_constructor('tag:yaml.org,2002:null',:construct_yaml_null)
  BaseConstructor::add_constructor('tag:yaml.org,2002:bool',:construct_yaml_bool)
  BaseConstructor::add_constructor('tag:yaml.org,2002:int',:construct_yaml_int)
  BaseConstructor::add_constructor('tag:yaml.org,2002:float',:construct_yaml_float)
  BaseConstructor::add_constructor('tag:yaml.org,2002:binary',:construct_yaml_binary)
  BaseConstructor::add_constructor('tag:yaml.org,2002:timestamp',:construct_yaml_timestamp)
  BaseConstructor::add_constructor('tag:yaml.org,2002:omap',:construct_yaml_omap)
  BaseConstructor::add_constructor('tag:yaml.org,2002:pairs',:construct_yaml_pairs)
  BaseConstructor::add_constructor('tag:yaml.org,2002:set',:construct_yaml_set)
  BaseConstructor::add_constructor('tag:yaml.org,2002:str',:construct_yaml_str)
  BaseConstructor::add_constructor('tag:yaml.org,2002:seq',:construct_yaml_seq)
  BaseConstructor::add_constructor('tag:yaml.org,2002:map',:construct_yaml_map)
  BaseConstructor::add_constructor(nil,:construct_private_type)

  BaseConstructor::add_multi_constructor("!ruby/object:",:construct_ruby)

  class Constructor < SafeConstructor
  end
end
