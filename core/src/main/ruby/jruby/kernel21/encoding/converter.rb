class << Encoding::Converter
  def search_convpath(from, to, options={})
    new(from, to, options).convpath
  end
end