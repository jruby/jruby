require 'java' #needed for the module JavaUtilities, which JavaEmbedUtils have a dependency on
require 'date'
require 'yaml_internal'

class Java::OrgJrubyYaml::JRubyRepresenter
  def kind_of?(other)
    if other == YAML::Emitter
      return true
    else
      super
    end
  end
end

module YAML
  #
  # Default settings
  #
  DEFAULTS = {
    :Indent => 2, :UseHeader => false, :UseVersion => false, :Version => '1.0',
    :SortKeys => false, :AnchorFormat => 'id%03d', :ExplicitTypes => false,
    :WidthType => 'absolute', :BestWidth => 80,
    :UseBlock => false, :UseFold => false, :Encoding => :None
  }

  class Error < StandardError; end

  def self.parse(obj)
#    Proxy.new(YAML::load(obj))
        YAML::JvYAML::Node::from_internal(YAML::_parse_internal(obj)) || false
  end

  def YAML.parse_file( filepath )
    File.open( filepath ) do |f|
      parse( f )
    end
  end

  def self.add_domain_type(*args)
      warn "YAML::add_domain_type isn't supported on JRuby"
  end

  def self.parse_documents(*args)
      warn "YAML::parse_documents isn't supported on JRuby"
  end
  
  class Proxy
    def initialize(v)
      @value = v
    end
    
    def transform
      @value
    end
  end

  class YPath
    def self.each_path(*args)
      warn "YAML::YPath.each_path isn't supported on JRuby"
    end
  end

  class Emitter
    def initialize
      @out = YAML::JvYAML::Out.new self
    end

    def reset(opts)
      @opts = opts
      self
    end

    def emit(oid, &proc)
      proc.call(@out)
    end
    
    def has_key?(key)
    end
  end

  class Object
    attr_accessor :class, :ivars
    def initialize(cl, iv)
      @class, @ivars = cl, iv
    end

    def to_yaml( opts = {} )
      YAML::quick_emit( object_id, opts ) do |out|
        out.map( "tag:ruby.yaml.org,2002:object:#{ @class }", to_yaml_style ) do |map|
          @ivars.each do |k,v|
            map.add( k, v )
          end
        end
      end
    end
  end
  
  def YAML.emitter; Emitter.new; end

  #
  # Allocate an Emitter if needed
  #
  def YAML.quick_emit( oid, opts = {}, &e )
    out = 
      if opts.is_a? YAML::Emitter
        opts
      else
        emitter.reset( opts )
      end
    out.emit( oid, &e )
  end
  
  module JvYAML
    class Out
      attr_accessor :emitter
      
      def initialize(emitter)
        @emitter = emitter
      end
      
      def map(type_id, style = nil)
        map = Map.new(type_id, {}, style)
        yield map
        map.to_s
      end

      def seq(type_id, style = nil)
        seq = Seq.new(type_id, [], style)
        yield seq
        seq
      end

      def scalar(type_id, str, style = nil)
        Scalar.new(type_id, str, style)
      end
    end
    
    class Node
      attr_accessor :value
      attr_accessor :style
      attr_accessor :type_id

      def transform
        org.jruby.yaml.JRubyConstructor.new(self, nil).construct_document(to_internal)
      end
      
      def to_str
        YAML.dump(self)
      end

      def to_s
        YAML.dump(self)
      end
      
      def self.from_internal(internal) 
        case internal
        when org.jvyamlb.nodes.ScalarNode
          Scalar.new(internal.tag, internal.value, internal.style.chr)
        when org.jvyamlb.nodes.MappingNode
          Map.new(internal.tag, internal.value.inject({}) {|h, obj| h[from_internal(obj[0])] = from_internal(obj[1]); h}, internal.flow_style)
        when org.jvyamlb.nodes.SequenceNode
          Seq.new(internal.tag, internal.value.map {|obj| from_internal(obj)}, internal.flow_style)
        end
      end
    end

    class Scalar < Node
      def initialize(type_id, val, style)
        @kind = :scalar
        self.type_id = type_id
        self.value = val
        self.style = style
      end
      
      def to_internal
        org.jvyamlb.nodes.ScalarNode.new(self.type_id, org.jruby.util.ByteList.new(self.value.to_java_bytes, false), self.style[0])
      end
      
      def to_yaml_node(repr)
        repr.scalar(self.type_id,self.value,self.style)
      end
    end

    class Seq < Node
      def initialize(type_id, val, style)
        @kind = :seq
        self.type_id = type_id
        self.value = val
        self.style = style
      end
      def add(v)
        @value << v
      end
      
      def to_internal
        org.jvyamlb.nodes.SequenceNode.new(self.type_id, self.value.map {|v| v.to_internal }, self.style)
      end
      
      def to_yaml_node(repr)
        repr.seq(self.type_id,self.value,self.style)
      end
    end

    class Map < Node
      def initialize(type_id, val, style)
        @kind = :map
        self.type_id = type_id
        self.value = val
        self.style = style
      end
      def add(k, v)
        @value[k] = v
      end

      def to_internal
        org.jvyamlb.nodes.MappingNode.new(self.type_id, self.value.inject({}) {|h, v| h[v[0].to_internal] = v[1].to_internal ;h }, self.style)
      end
      
      def to_yaml_node(repr)
        repr.map(self.type_id,self.value,self.style)
      end
    end
  end
  
  #
  # YAML::Stream -- for emitting many documents
  #
  class Stream
    include Enumerable
    attr_accessor :documents, :options
    def initialize(opts = {})
      @options = opts
      @documents = []
    end
    
    def [](i)
      @documents[ i ]
    end
    
    def add(doc)
      @documents << doc
    end

    def edit(doc_num,doc)
      @documents[ doc_num ] = doc
    end

    def each(&block)
      @documents.each(&block)
    end
    
    def emit
      YAML::dump_all(@documents)
    end
  end

    #
    # Default private type
    #
    class PrivateType
        def self.tag_subclasses?; false; end
        attr_accessor :type_id, :value
        verbose, $VERBOSE = $VERBOSE, nil
        def initialize( type, val )
            @type_id = type; @value = val
        end
        def to_yaml_node(repr)
          @value.to_yaml_node(repr)
        end
    ensure
        $VERBOSE = verbose
    end

    #
    # Convert a type_id to a taguri
    #
    def YAML.tagurize( val )
      if /^tag:.*?:.*$/ =~ val.to_s
        val
      elsif /^(.*?)\/(.*)$/ =~ val.to_s
        "tag:#$1.yaml.org,2002:#$2"
      elsif val.kind_of?(Integer)
        val
      else
        "tag:yaml.org,2002:#{val}"
      end
    end
      
# From yaml/tag.rb
    # A dictionary of taguris which map to
    # Ruby classes.
    @@tagged_classes = {}
    
    # 
    # Associates a taguri _tag_ with a Ruby class _cls_.  The taguri is used to give types
    # to classes when loading YAML.  Taguris are of the form:
    #
    #   tag:authorityName,date:specific
    #
    # The +authorityName+ is a domain name or email address.  The +date+ is the date the type
    # was issued in YYYY or YYYY-MM or YYYY-MM-DD format.  The +specific+ is a name for
    # the type being added.
    # 
    # For example, built-in YAML types have 'yaml.org' as the +authorityName+ and '2002' as the
    # +date+.  The +specific+ is simply the name of the type:
    #
    #  tag:yaml.org,2002:int
    #  tag:yaml.org,2002:float
    #  tag:yaml.org,2002:timestamp
    #
    # The domain must be owned by you on the +date+ declared.  If you don't own any domains on the
    # date you declare the type, you can simply use an e-mail address.
    #
    #  tag:why@ruby-lang.org,2004:notes/personal
    #
    def YAML.tag_class( tag, cls )
        if @@tagged_classes.has_key? tag
            warn "class #{ @@tagged_classes[tag] } held ownership of the #{ tag } tag"
        end
        @@tagged_classes[tag] = cls
    end

    # Returns the complete dictionary of taguris, paired with classes.  The key for
    # the dictionary is the full taguri.  The value for each key is the class constant
    # associated to that taguri.
    #
    #  YAML.tagged_classes["tag:yaml.org,2002:int"] => Integer
    #
    def YAML.tagged_classes
        @@tagged_classes
    end
end

# From yaml/tag.rb
class Module
    # :stopdoc:

    # Adds a taguri _tag_ to a class, used when dumping or loading the class
    # in YAML.  See YAML::tag_class for detailed information on typing and
    # taguris.
    def yaml_as( tag, sc = true )
        verbose, $VERBOSE = $VERBOSE, nil
        class_eval <<-"end;", __FILE__, __LINE__+1
            attr_writer :taguri
            def taguri
                if respond_to? :to_yaml_type
                    YAML::tagurize( to_yaml_type[1..-1] )
                else
                    return @taguri if defined?(@taguri) and @taguri
                    tag = #{ tag.dump }
                    if self.class.yaml_tag_subclasses? and self.class != YAML::tagged_classes[tag]
                        tag = "\#{ tag }:\#{ self.class.yaml_tag_class_name }"
                    end
                    tag
                end
            end
            def self.yaml_tag_subclasses?; #{ sc ? 'true' : 'false' }; end
        end;
        YAML::tag_class tag, self
    ensure
        $VERBOSE = verbose
    end
    # Transforms the subclass name into a name suitable for display
    # in a subclassed tag.
    def yaml_tag_class_name
        self.name
    end
    # Transforms the subclass name found in the tag into a Ruby
    # constant name.
    def yaml_tag_read_class( name )
        name
    end
end

  Hash::yaml_as "tag:ruby.yaml.org,2002:hash"
  Hash::yaml_as "tag:yaml.org,2002:map"

  Array::yaml_as "tag:ruby.yaml.org,2002:array"
  Array::yaml_as "tag:yaml.org,2002:seq"

  String::yaml_as "tag:ruby.yaml.org,2002:string"
  String::yaml_as "tag:yaml.org,2002:binary"
  String::yaml_as "tag:yaml.org,2002:str"

  Range::yaml_as "tag:ruby.yaml.org,2002:range"
  
  Regexp::yaml_as "tag:ruby.yaml.org,2002:regexp"

  Integer::yaml_as "tag:yaml.org,2002:int", false

  Time::yaml_as "tag:ruby.yaml.org,2002:time"
  Time::yaml_as "tag:yaml.org,2002:timestamp"

  Date::yaml_as "tag:yaml.org,2002:timestamp#ymd"

  Float::yaml_as "tag:yaml.org,2002:float"

  NilClass::yaml_as "tag:yaml.org,2002:null"

  YAML::tag_class "tag:yaml.org,2002:bool#yes", TrueClass
  YAML::tag_class "tag:yaml.org,2002:bool#no", FalseClass
  YAML::tag_class "tag:ruby.yaml.org,2002:object", Object
  YAML::tag_class "tag:ruby.yaml.org,2002:exception", Exception
  YAML::tag_class "tag:ruby.yaml.org,2002:struct", Struct
  YAML::tag_class "tag:ruby.yaml.org,2002:symbol", Symbol
  YAML::tag_class "tag:ruby.yaml.org,2002:sym", Symbol

# From yaml/types.rb
module YAML
    #
    # Builtin collection: !omap
    #
    class Omap < ::Array
        yaml_as "tag:yaml.org,2002:omap"
        def self.[]( *vals )
            o = Omap.new
            0.step( vals.length - 1, 2 ) do |i|
                o[vals[i]] = vals[i+1]
            end
            o
        end
        def []( k )
            self.assoc( k ).to_a[1]
        end
        def []=( k, *rest )
            val, set = rest.reverse
            if ( tmp = self.assoc( k ) ) and not set
                tmp[1] = val
            else
                self << [ k, val ] 
            end
            val
        end
        def has_key?( k )
            self.assoc( k ) ? true : false
        end
        def is_complex_yaml?
            true
        end
        def to_yaml_node(repr)
          sequ = []
          self.each do |v|
            sequ << Hash[ *v ]
          end
          
          repr.seq(taguri,sequ,to_yaml_style)
        end
    end

    
        #
    # Builtin collection: !pairs
    #
    class Pairs < ::Array
        yaml_as "tag:yaml.org,2002:pairs"
        def self.[]( *vals )
            p = Pairs.new
            0.step( vals.length - 1, 2 ) { |i|
                p[vals[i]] = vals[i+1]
            }
            p
        end
        def []( k )
            self.assoc( k ).to_a
        end
        def []=( k, val )
            self << [ k, val ] 
            val
        end
        def has_key?( k )
            self.assoc( k ) ? true : false
        end
        def is_complex_yaml?
            true
        end
        def to_yaml_node(repr)
          sequ = []
          self.each do |v|
            sequ << Hash[ *v ]
          end
          repr.seq(taguri,sequ,to_yaml_style)
        end
    end

    #
    # Builtin collection: !set
    #
    class Set < ::Hash
        yaml_as "tag:yaml.org,2002:set"
    end
end
  
require 'yaml/syck'
