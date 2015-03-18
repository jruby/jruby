not_supported_on :ironruby do
  require 'generator'
end

module GeneratorSpecs
  def self.empty
    Generator.new([])
  end

  def self.one_elem
    Generator.new([1])
  end

  def self.two_elems
    Generator.new([1, 2])
  end

  def self.four_elems
    Generator.new(['A', 'B', 'C', 'Z'])
  end
end
