
require 'rbyaml/yaml'

module RbYAML
  def self.dump(obj, io = nil)
    _dump(obj,io)
  end

  def self.load( io )
    _load(io)
  end

  def self.load_file( filepath )
    File.open( filepath ) do |f|
      load( f )
    end
  end

  # this operation does not make sense in RbYAML (right now)
  def self.parse( io )
    #    yp = @@parser.new( :Model => :Generic ).load( io )
  end

  # this operation does not make sense in RbYAML (right now)
  def self.parse_file( filepath )
    #    File.open( filepath ) do |f|
    #      parse( f )
    #    end
  end

  def self.each_document( io, &block )
    _load_all(io,&block)
  end

  def self.load_documents( io, &doc_proc )
    each_document( io, &doc_proc )
  end

  # this operation does not make sense in RbYAML (right now)
  def self.each_node( io, &doc_proc )
    #    yp = @@parser.new( :Model => :Generic ).load_documents( io, &doc_proc )
  end

  # this operation does not make sense in RbYAML (right now)
  def self.parse_documents( io, &doc_proc )
    #    YAML.each_node( io, &doc_proc )
  end
  
  def self.load_stream( io )
    d = nil
    load_documents(io) { |doc|
      d = Stream.new( nil ) if not d
      d.add( doc ) 
    }
    d
  end

  def self.dump_stream( *objs )
    d = RbYAML::Stream.new
    objs.each do |doc|
      d.add( doc ) 
    end
    d.emit
  end


  def self.add_builtin_ctor(type_tag, &transfer_proc)
    BaseConstructor::add_constructor("tag:yaml.org,2002:#{ type_tag }",transfer_proc)
  end

  # this operation does not make sense in RbYAML (right now)
  def self.add_domain_type( domain, type_re, &transfer_proc )
    #    @@loader.add_domain_type( domain, type_re, &transfer_proc )
  end

  # this operation does not make sense in RbYAML (right now)
  def self.add_builtin_type( type_re, &transfer_proc )
    #    @@loader.add_builtin_type( type_re, &transfer_proc )
  end

  # this operation does not make sense in RbYAML (right now)
  def self.add_ruby_type( type_tag, &transfer_proc )
    #    @@loader.add_ruby_type( type, &transfer_proc )
  end

  # this operation does not make sense in RbYAML (right now)
  def self.add_private_type( type_re, &transfer_proc )
    #    @@loader.add_private_type( type_re, &transfer_proc )
  end

  def self.detect_implicit( val )
    SimpleDetector.detect(val)
  end

  # this operation does not make sense in RbYAML (right now)
  def self.transfer( type_id, obj )
    #    @@loader.transfer( type_id, obj )
  end

  # this operation does not make sense in RbYAML (right now)
  def self.try_implicit( obj )
    #    YAML.transfer( YAML.detect_implicit( obj ), obj )
  end

  def self.read_type_class( type, obj_class )
    scheme, domain, type, tclass = type.split( ':', 4 )
    tclass.split( "::" ).each { |c| obj_class = obj_class.const_get( c ) } if tclass
    return [ type, obj_class ]
  end

  def self.object_maker( obj_class, val )
    if Hash === val
      o = obj_class.allocate
      val.each_pair { |k,v|
        o.instance_variable_set("@#{k}", v)
      }
      o
    else
      raise YAMLError, "Invalid object explicitly tagged !ruby/Object: " + val.inspect
    end
  end

  # this operation does not make sense in RbYAML (right now)
  def self.quick_emit( oid, opts = {}, &e )
  end

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
  def self.tag_class( tag, cls )
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
  def self.tagged_classes
    @@tagged_classes
  end

  #
  # RbYAML::Stream -- for emitting many documents
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
# TODO: implement

      opts = @options.dup
      opts[:UseHeader] = true if @documents.length > 1
      ct = 0
      out = Emitter.new( opts )
      @documents.each { |v|
        if ct > 0
          out << "\n--- " 
        end
        v.to_yaml( :Emitter => out )
        ct += 1
      }
      out.end_object
    end
  end
end

if !Object.method_defined?(:to_yaml)
  class Module # :nodoc: all
    def yaml_as( tag, sc = true )
      class_eval <<-"end;", __FILE__, __LINE__+1
      attr_writer :taguri
      def taguri
        return @taguri if defined?(@taguri) and @taguri
        tag = #{ tag.dump }
          if self.class.yaml_tag_subclasses? and self.class != RbYAML::tagged_classes[tag]
            tag = "\#{ tag }:\#{ self.class.yaml_tag_class_name }"
          end
        tag
      end
      def self.yaml_tag_subclasses?; #{ sc ? 'true' : 'false' }; end
      end;
      RbYAML::tag_class tag, self
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

  require 'date'

  class Class
    def to_yaml( opts = {} )
      raise RbYAML::TypeError, "can't dump anonymous class %s" % self.class
    end
  end

  class Object
    yaml_as "tag:ruby.yaml.org,2002:object"
    def is_complex_yaml?; true; end
    def to_yaml_style; end
    def to_yaml_properties; instance_variables.sort; end
    def to_yaml( opts = {} )
      RbYAML::_dump_ruby_object(self)
    end
  end

  class Hash
    yaml_as "tag:ruby.yaml.org,2002:hash"
    yaml_as "tag:yaml.org,2002:map"
    def is_complex_yaml?; true; end
    def yaml_initialize( tag, val )
      if Array === val
        update Hash.[]( *val )		# Convert the map to a sequence
      elsif Hash === val
        update val
      else
        raise RbYAML::TypeError, "Invalid map explicitly tagged #{ tag }: " + val.inspect
      end
    end
  end

  class Array
    yaml_as "tag:ruby.yaml.org,2002:array"
    yaml_as "tag:yaml.org,2002:seq"
    def is_complex_yaml?; true; end
    def yaml_initialize( tag, val ); concat( val.to_a ); end
  end

  class Exception
    yaml_as "tag:ruby.yaml.org,2002:exception"
    def Exception.yaml_new( klass, tag, val )
      o = RbYAML.object_maker( klass, { 'mesg' => val.delete( 'message' ) } )
      val.each_pair do |k,v|
        o.instance_variable_set("@#{k}", v)
      end
      o
    end
  end

  class String
    yaml_as "tag:ruby.yaml.org,2002:string"
    yaml_as "tag:yaml.org,2002:binary"
    yaml_as "tag:yaml.org,2002:str"
    def is_complex_yaml?
      to_yaml_style or not to_yaml_properties.empty? or self =~ /\n.+/
    end
    def is_binary_data?
      ( self.count( "^ -~", "^\r\n" ) / self.size > 0.3 || self.count( "\x00" ) > 0 ) unless empty?
    end
    def String.yaml_new( klass, tag, val )
      val = val.unpack("m")[0] if tag == "tag:yaml.org,2002:binary"
      val = { 'str' => val } if String === val
      if Hash === val
        s = klass.allocate
        # Thank you, NaHi
        String.instance_method(:initialize).
          bind(s).
          call( val.delete( 'str' ) )
        val.each { |k,v| s.instance_variable_set( k, v ) }
        s
      else
        raise RbYAML::TypeError, "Invalid String: " + val.inspect
      end
    end
  end

  class Symbol
    yaml_as "tag:ruby.yaml.org,2002:symbol"
    yaml_as "tag:ruby.yaml.org,2002:sym"
    def is_complex_yaml?; false; end
    def Symbol.yaml_new( klass, tag, val )
      if String === val
        val.intern
      else
        raise RbYAML::TypeError, "Invalid Symbol: " + val.inspect
      end
    end
  end

  class Time
    yaml_as "tag:ruby.yaml.org,2002:time"
    yaml_as "tag:yaml.org,2002:timestamp"
    def is_complex_yaml?; false; end
    def Time.yaml_new( klass, tag, val )
      if Hash === val
        t = val.delete( 'at' )
        val.each { |k,v| t.instance_variable_set( k, v ) }
        t
      else
        raise RbYAML::TypeError, "Invalid Time: " + val.inspect
      end
    end
  end

  class Date
    yaml_as "tag:yaml.org,2002:timestamp#ymd"
    def is_complex_yaml?; false; end
  end

  class Numeric
    def is_complex_yaml?; false; end
  end

  class Fixnum
    yaml_as "tag:yaml.org,2002:int"
  end

  class Float
    yaml_as "tag:yaml.org,2002:float"
  end

  class TrueClass
    yaml_as "tag:yaml.org,2002:bool#yes"
    def is_complex_yaml?; false; end
  end

  class FalseClass
    yaml_as "tag:yaml.org,2002:bool#no"
    def is_complex_yaml?; false; end
  end

  class NilClass 
    yaml_as "tag:yaml.org,2002:null"
    def is_complex_yaml?; false; end
  end
end
