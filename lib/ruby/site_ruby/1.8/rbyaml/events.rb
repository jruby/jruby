
module RbYAML
  Event = Struct.new(:start_mark,:end_mark)
  class Event
    def hash
      object_id
    end
    def to_s
      attributes = ["@anchor","@tag","@implicit","@value"] & self.instance_variables
      args = attributes.collect {|val| "#{val[1..-1]}=" + eval("#{val}").to_s}.join(", ")
      "#{self.class.name}(#{args})"
    end
    def __is_node; false; end
    def __is_collection_start; false; end
    def __is_collection_end; false; end
    def __is_stream_start; false; end
    def __is_stream_end; false; end
    def __is_document_start; false; end
    def __is_document_end; false; end
    def __is_alias; false; end
    def __is_scalar; false; end
    def __is_sequence_start; false; end
    def __is_sequence_end; false; end
    def __is_mapping_start; false; end
    def __is_mapping_end; false; end
  end
  
  class NodeEvent < Event
    attr_reader :anchor
    def initialize(anchor, start_mark=nil, end_mark=nil)
      super(start_mark,end_mark)
      @anchor = anchor
    end
    def __is_node; true; end
  end

  class CollectionStartEvent < NodeEvent
    attr_reader :tag, :implicit, :flow_style
    def initialize(anchor,tag,implicit,start_mark=nil, end_mark=nil,flow_style=nil)
      super(anchor,start_mark,end_mark)
      @tag = tag
      @implicit = implicit
      @flow_style = flow_style        
    end
    def __is_collection_start; true; end
  end

  class CollectionEndEvent < Event
    def __is_collection_end; true; end
  end

  class StreamStartEvent < Event
    attr_reader :encoding
    def initialize(start_mark=nil,end_mark=nil,encoding=nil)
      super(start_mark,end_mark)
      @encoding = encoding
    end
    def __is_stream_start; true; end
  end

  class StreamEndEvent < Event
    def __is_stream_end; true; end
  end

  class DocumentStartEvent < Event
    attr_reader :explicit, :version, :tags
    def initialize(start_mark=nil,end_mark=nil,explicit=nil,version=nil,tags=nil)
      super(start_mark,end_mark)
      @explicit = explicit
      @version = version
      @tags = tags
    end
    def __is_document_start; true; end
  end

  class DocumentEndEvent < Event
    attr_reader :explicit
    def initialize(start_mark=nil,end_mark=nil,explicit=nil)
      super(start_mark,end_mark)
      @explicit = explicit
    end
    def __is_document_end; true; end
  end

  class AliasEvent < NodeEvent
    def __is_alias; true; end
  end

  class ScalarEvent < NodeEvent
    attr_reader :tag, :style, :value, :implicit
    def initialize(anchor,tag,implicit,value,start_mark=nil, end_mark=nil,style=nil)
      super(anchor,start_mark,end_mark)
      @tag = tag
      @style = style
      @value = value
      @implicit = implicit
    end
    def __is_scalar; true; end
  end

  class SequenceStartEvent < CollectionStartEvent
    def __is_sequence_start; true; end
  end

  class SequenceEndEvent < CollectionEndEvent
    def __is_sequence_end; true; end
  end

  class MappingStartEvent < CollectionStartEvent
    def __is_mapping_start; true; end
  end

  class MappingEndEvent < CollectionEndEvent
    def __is_mapping_end; true; end
  end
end

