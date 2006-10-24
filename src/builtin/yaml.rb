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
end
  
