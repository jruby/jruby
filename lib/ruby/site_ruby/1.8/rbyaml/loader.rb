# This is a more or less straight translation of PyYAML3000 to Ruby

require 'rbyaml/scanner'
require 'rbyaml/parser'
require 'rbyaml/composer'
require 'rbyaml/constructor'
require 'rbyaml/resolver'

module RbYAML
  class CommonLoader
    attr_accessor :scanner, :parser, :composer, :constructor, :resolver

    def initialize(stream,scanner=Scanner,parser=Parser,composer=Composer,constructor=BaseConstructor,resolver=BaseResolver)
      @scanner = scanner.new(stream)
      @parser = parser.new(@scanner)
      @resolver = resolver.new
      @composer = composer.new(@parser,@resolver)
      @constructor = constructor.new(@composer)
    end
  end
  
  class BaseLoader < CommonLoader
    def initialize(stream)
      super(stream,Scanner,Parser,Composer,BaseConstructor,BaseResolver)
    end
  end
  
  class SafeLoader < CommonLoader
    def initialize(stream)
      super(stream,Scanner,Parser,Composer,SafeConstructor,Resolver)
    end
  end
  
  class Loader < CommonLoader
    def initialize(stream)
      super(stream,Scanner,Parser,Composer,Constructor,Resolver)
    end
  end
end

