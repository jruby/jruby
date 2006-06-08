
module RbYAML
  Token = Struct.new(:start_mark, :end_mark)

  class Token
    def __is_document_start; false; end
    def __is_document_end; false; end
    def __is_stream_start; false; end
    def __is_stream_end; false; end
    def __is_directive; false; end
    def __is_block_sequence_start; false; end
    def __is_block_mapping_start; false; end
    def __is_block_end; false; end
    def __is_flow_sequence_start; false; end
    def __is_flow_mapping_start; false; end
    def __is_flow_sequence_end; false; end
    def __is_flow_mapping_end; false; end
    def __is_key; false; end
    def __is_value; false; end
    def __is_block_entry; false; end
    def __is_flow_entry; false; end
    def __is_alias; false; end
    def __is_anchor; false; end
    def __is_tag; false; end
    def __is_scalar; false; end
    def hash
      object_id
    end
  end

  class DirectiveToken < Token
    def __is_directive; true; end
    def tid
      "<directive>"
    end
    attr_reader :name, :value
    def initialize(name, value, start_mark, end_mark)
      super(start_mark,end_mark)
      @name = name
      @value = value
    end
  end

  class DocumentStartToken < Token
    def __is_document_start; true; end
    def tid
      "<document start>"
    end
  end

  class DocumentEndToken < Token
    def __is_document_end; true; end
    def tid
      "<document end>"
    end
  end

  class StreamStartToken < Token
    def __is_stream_start; true; end
    def tid
      "<stream start>"
    end
    attr_reader :encoding
    def initialize(start_mark=nil, end_mark=nil, encoding=nil)
      super(start_mark,end_mark)
      @encoding = encoding
    end
  end
  
  class StreamEndToken < Token
    def __is_stream_end; true; end
    def tid
      "<stream end>"
    end
  end

  class BlockSequenceStartToken < Token
    def __is_block_sequence_start; true; end
    def tid
      "<block sequence start>"
    end
  end

  class BlockMappingStartToken < Token
    def __is_block_mapping_start; true; end
    def tid
      "<block mapping start>"
    end
  end

  class BlockEndToken < Token
    def __is_block_end; true; end
    def tid
      "<block end>"
    end
  end

  class FlowSequenceStartToken < Token
    def __is_flow_sequence_start; true; end
    def tid
      "["
    end
  end

  class FlowMappingStartToken < Token
    def __is_flow_mapping_start; true; end
    def tid
      "{"
    end
  end

  class FlowSequenceEndToken < Token
    def __is_flow_sequence_end; true; end
    def tid
      "]"
    end
  end

  class FlowMappingEndToken < Token
    def __is_flow_mapping_end; true; end
    def tid
      "}"
    end
  end

  class KeyToken < Token
    def __is_key; true; end
    def tid
      "?"
    end
  end

  class ValueToken < Token
    def __is_value; true; end
    def tid
      ":"
    end
  end

  class BlockEntryToken < Token
    def __is_block_entry; true; end
    def tid
      "-"
    end
  end

  class FlowEntryToken < Token
    def __is_flow_entry; true; end
    def tid
      ","
    end
  end

  class AliasToken < Token
    def __is_alias; true; end
    def tid
      "<alias>"
    end
    attr_reader :value
    def initialize(value, start_mark, end_mark)
      super(start_mark,end_mark)
      @value = value
    end
  end

  class AnchorToken < Token
    def __is_anchor; true; end
    def tid
      "<anchor>"
    end
    attr_reader :value
    def initialize(value, start_mark, end_mark)
      super(start_mark,end_mark)
      @value = value
    end
  end

  class TagToken < Token
    def __is_tag; true; end
    def tid
      "<tag>"
    end
    attr_reader :value
    def initialize(value, start_mark, end_mark)
      super(start_mark,end_mark)
      @value = value
    end
  end

  class ScalarToken < Token
    def __is_scalar; true; end
    def tid
      "<scalar>"
    end
    attr_reader :value, :plain, :style
    alias :implicit :plain #Until all references to ScalarToken.implicit has been removed
    def initialize(value, plain, start_mark, end_mark, style=nil)
      super(start_mark, end_mark)
      @value = value
      @plain = plain
      @style = style
    end
  end
end
