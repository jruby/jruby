module Duby::AST
  class Import < Node
    attr_accessor :short
    attr_accessor :long
    def initialize(parent, short, long)
      @short = short
      @long = long
      super(parent, [])
    end

    def to_s
      "Import(#{short} = #{long})"
    end

    def infer(typer)
      # add both the meta and non-meta imports
      typer.known_types[TypeReference.new(short, false, true)] = TypeReference.new(long, false, true)
      typer.known_types[TypeReference.new(short, false, false)] = TypeReference.new(long, false, false)
      TypeReference::NoType
    end
  end
end