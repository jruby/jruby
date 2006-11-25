require 'java' #needed for the module JavaUtilities, which JavaEmbedUtils have a dependency on
require 'yaml_internal'

module YAML
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
  
