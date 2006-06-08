require 'rbyaml/emitter'
require 'rbyaml/serializer'
require 'rbyaml/representer'
require 'rbyaml/resolver'

module RbYAML
  class CommonDumper
    attr_accessor :emitter, :serializer, :representer, :resolver
    def initialize(stream,default_style=nil,default_flow_style=nil,canonical=nil,indent=nil,width=nil,line_break=nil,explicit_start=nil,explicit_end=nil,version=nil,tags=nil,emitter=Emitter,serializer=Serializer,representer=Representer,resolver=Resolver)
      super()
      @emitter = emitter.new(stream,canonical,indent,width,line_break)
      @resolver = resolver.new
      @serializer = serializer.new(@emitter,@resolver,explicit_start,explicit_end,version,tags)
      @representer = representer.new(@serializer,default_style,default_flow_style)
    end
  end
  
  class BaseDumper < CommonDumper
    attr_accessor 
    def initialize(stream,default_style=nil,default_flow_style=nil,canonical=nil,indent=nil,width=nil,line_break=nil,explicit_start=nil,explicit_end=nil,version=nil,tags=nil,emitter=Emitter,serializer=Serializer,representer=BaseRepresenter,resolver=BaseResolver)
      super
    end
  end
  
  class SafeDumper < CommonDumper
    def initialize(stream,default_style=nil,default_flow_style=nil,canonical=nil,indent=nil,width=nil,line_break=nil,explicit_start=nil,explicit_end=nil,version=nil,tags=nil,emitter=Emitter,serializer=Serializer,representer=SafeRepresenter,resolver=Resolver)
      super
    end
  end
  
  class Dumper < CommonDumper
    def initialize(stream,default_style=nil,default_flow_style=nil,canonical=nil,indent=nil,width=nil,line_break=nil,explicit_start=nil,explicit_end=nil,version=nil,tags=nil,emitter=Emitter,serializer=Serializer,representer=Representer,resolver=Resolver)
      super
    end
  end
end
