require 'mspec/utils/version'

module StringSymbolAdapter
  def convert_name(name)
    version = SpecVersion.new(RUBY_VERSION) <=> "1.9"
    version < 0 ? name.to_s : name.to_sym
  end
end
