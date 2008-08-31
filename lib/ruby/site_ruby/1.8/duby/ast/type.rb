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
      typer.known_types[short] = TypeReference.new(long, false, true)

      TypeReference::NoType
    end
  end
end