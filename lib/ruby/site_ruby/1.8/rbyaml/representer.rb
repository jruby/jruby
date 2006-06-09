
require 'set'

require 'rbyaml/error'
require 'rbyaml/nodes'

module RbYAML
  class RepresenterError < YAMLError
  end

  class BaseRepresenter
    @@yaml_representers = {}
    @@yaml_multi_representers = {}

    def initialize(serializer, default_style=nil, default_flow_style=nil)
      @serializer = serializer
      @default_style = default_style
      @default_flow_style = default_flow_style
      @represented_objects = {}
    end

    def represent(data)
      node = represent_data(data)
      @serializer.serialize(node)
      represented_objects = {}
    end
    
    CLASSOBJ_TYPE = Class
    INSTANCE_TYPE = Object
    FUNCTION_TYPE = Method
    BUILTIN_FUNCTION_TYPE = Method
    MODULE_TYPE = Module

    def get_classobj_bases(cls)
      [cls] + cls.ancestors
    end
    
    def represent_data(data)
      if ignore_aliases(data)
        alias_key = nil
      else
        alias_key = data.object_id
      end
      
      if !alias_key.nil?
        if @represented_objects.include?(alias_key)
          node = @represented_objects[alias_key]
          raise RepresenterError.new("recursive objects are not allowed: #{data}") if node.nil?
          return node
        end
        @represented_objects[alias_key] = nil
      end
      
      data_types = data.class.ancestors
      data_types = get_classobj_bases(data.class) + data_types if INSTANCE_TYPE === data

      if @@yaml_representers.include?(data_types[0])
        node = send(@@yaml_representers[data_types[0]],data)
      else
        rerun = true
        for data_type in data_types
          if @@yaml_multi_representers.include?(data_type)
            node = send(@@yaml_multi_representers[data_type],data)
            rerun = false
            break
          elsif @@yaml_representers.include?(data_type)
            node = send(@@yaml_representers[data_type],data)
            rerun = false
            break
          end
        end
        if rerun
          if @@yaml_multi_representers.include?(nil)
            node = send(@@yaml_multi_representers[nil],data)
          elsif @@yaml_representers.include?(nil)
            node = send(@@yaml_representers[nil],data)
          else
            node = ScalarNode.new(nil, data)
          end
        end
      end
        
      @represented_objects[alias_key] = node if !alias_key.nil?
      node
    end
    
    def self.add_representer(data_type, representer)
      @@yaml_representers[data_type] = representer
    end

    def self.add_multi_representer(data_type, representer)
      @@yaml_multi_representers[data_type] = representer
    end
    
    def represent_scalar(tag, value, style=nil)
      style ||= @default_style
      ScalarNode.new(tag,value,nil,nil,style)
    end

    def represent_sequence(tag, sequence, flow_style=nil)
      best_style = true
      value = sequence.map {|item| 
        node_item = represent_data(item)
        best_style = false if !node_item.__is_scalar && !node_item.flow_style
        node_item
      }
      flow_style ||= (@flow_default_style || best_style)
      SequenceNode.new(tag, value, flow_style)
    end

    def represent_mapping(tag, mapping, flow_style=nil)
      best_style = true
      if mapping.respond_to?(:keys)
        value = {}
        for item_key,item_value in mapping
          node_key = represent_data(item_key)
          node_value = represent_data(item_value)
          best_style = false if !node_key.__is_scalar && !node_key.flow_style
          best_style = false if !node_value.__is_scalar && !node_value.flow_style
          value[node_key] = node_value
        end
      else
        value = []
        for item_key, item_value in mapping
          node_key = represent_data(item_key)
          node_value = represent_data(item_value)
          best_style = false if !node_key.__is_scalar && !node_key.flow_style
          best_style = false if !node_value.__is_scalar && !node_value.flow_style
          value << [node_key, node_value]
        end
      end
      flow_style ||= (@flow_default_style || best_style)
      MappingNode.new(tag, value, flow_style)
    end

    def ignore_aliases(data)
      false
    end
  end

  class SafeRepresenter < BaseRepresenter

    def ignore_aliases(data)
      data.nil? || data.__is_str || TrueClass === data || FalseClass === data || data.__is_int || Float === data
    end

    def represent_none(data)
#      represent_scalar(data.taguri,"null")
      represent_scalar('tag:yaml.org,2002:str',"")
    end

    NON_PRINTABLE = /[^\x09\x0A\x0D\x20-\x7E\x85\xA0-\xFF]/
    def represent_str(data)
      style = nil
      if NON_PRINTABLE =~ data
        data = Base64.encode64(data)
        data.taguri ="tag:yaml.org,2002:binary"
        style = "|"
      end
      represent_scalar(data.taguri,data,style)
    end

    def represent_symbol(data)
      represent_scalar(data.taguri,data.to_s)
    end

    def represent_private_type(data)
      represent_scalar(data.type_id,data.value)
    end

    def represent_bool(data)
      value = data ? "true" : "false"
      represent_scalar('tag:yaml.org,2002:bool',value)
    end

    def represent_int(data)
      represent_scalar(data.taguri,data.to_s)
    end

    def represent_float(data)
      if data.infinite? == 1
        value = ".inf"
      elsif data.infinite? == -1
        value = "-.inf"
      elsif data.nan? || data != data
        value = ".nan"
      else
        value = data.to_s
      end
      represent_scalar(data.taguri, value)
    end
    
    def represent_list(data)
#no support for pairs right now. should probably be there, though...
      represent_sequence(data.taguri, data)
    end

    def represent_dict(data)
      represent_mapping(data.taguri, data)
    end

    def represent_set(data)
      value = {}
      for key in data
        value[key] = nil
      end
      represent_mapping(data.taguri, value)
    end
    
    def represent_datetime(data)
      tz = "Z"
      # from the tidy Tobias Peters <t-peters@gmx.de> Thanks!
      unless data.utc?
        utc_same_instant = data.dup.utc
        utc_same_writing = Time.utc(data.year,data.month,data.day,data.hour,data.min,data.sec,data.usec)
        difference_to_utc = utc_same_writing - utc_same_instant
        if (difference_to_utc < 0) 
          difference_sign = '-'
          absolute_difference = -difference_to_utc
        else
          difference_sign = '+'
          absolute_difference = difference_to_utc
        end
        difference_minutes = (absolute_difference/60).round
        tz = "%s%02d:%02d" % [ difference_sign, difference_minutes / 60, difference_minutes % 60]
      end
      standard = data.strftime( "%Y-%m-%d %H:%M:%S" )
      standard += ".%06d" % [data.usec] if data.usec.nonzero?
      standard += " %s" % [tz]
      represent_scalar(data.taguri, standard)
    end
    
    def represent_ruby(data)
      state = data.to_yaml_properties
      map = {}
      state.each do |m|
        map[m[1..-1]] = data.instance_variable_get(m)
      end
      represent_mapping("!ruby/object:#{data.class.yaml_tag_class_name}", map,nil)
    end
    
    def represent_yaml_object(tag, data, flow_style=nil)
      state = data.to_yaml_properties
      map = {}
      state.each do |m|
        map[m[1..-1]] = data.instance_variable_get(m)
      end
      represent_mapping(tag, map, flow_style)
    end

    def represent_undefined(data)
      raise RepresenterError.new("cannot represent an object: #{data}")
    end
  end

  BaseRepresenter.add_representer(NilClass,:represent_none)
  BaseRepresenter.add_representer(String,:represent_str)
  BaseRepresenter.add_representer(Symbol,:represent_symbol)
  BaseRepresenter.add_representer(TrueClass,:represent_bool)
  BaseRepresenter.add_representer(FalseClass,:represent_bool)
  BaseRepresenter.add_representer(Integer,:represent_int)
  BaseRepresenter.add_representer(Float,:represent_float)
  BaseRepresenter.add_representer(Array,:represent_list)
  BaseRepresenter.add_representer(Hash,:represent_dict)
  BaseRepresenter.add_representer(Set,:represent_set)
  BaseRepresenter.add_representer(Time,:represent_datetime)
  BaseRepresenter.add_representer(PrivateType,:represent_private_type)
  BaseRepresenter.add_representer(Object,:represent_ruby)
  BaseRepresenter.add_representer(nil,:represent_undefined)
  
  class Representer < SafeRepresenter
  end
end
