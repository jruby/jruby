class Encoding
  # There's a fun bootstrapping issue here.  Encoding::Converter.transcoding_map needs access to the Encoding::Transcoding
  # class. However, this shim runs before Encoding::Transcoding is defined.  Since the class body is short, we reproduce
  # it here along with a convenient factory method.
  class Transcoding
    attr_accessor :source
    attr_accessor :target

    def inspect
      "#<#{super} #{source} to #{target}"
    end

    def self.create(source, target)
      ret = new

      ret.source = source
      ret.target = target

      ret
    end
  end

  TranscodingMap = Encoding::Converter.transcoding_map
  EncodingMap = Encoding.encoding_map
  EncodingList = Encoding.list

  @default_external = undefined
  @default_internal = undefined
end

Encoding::TranscodingMap[:'UTF-16BE'] = Rubinius::LookupTable.new
Encoding::TranscodingMap[:'UTF-16BE'][:'UTF-8'] = nil

Encoding::TranscodingMap[:'UTF-16LE'] = Rubinius::LookupTable.new
Encoding::TranscodingMap[:'UTF-16LE'][:'UTF-8'] = nil

Encoding::TranscodingMap[:'UTF-32BE'] = Rubinius::LookupTable.new
Encoding::TranscodingMap[:'UTF-32BE'][:'UTF-8'] = nil

Encoding::TranscodingMap[:'UTF-32LE'] = Rubinius::LookupTable.new
Encoding::TranscodingMap[:'UTF-32LE'][:'UTF-8'] = nil

Encoding::TranscodingMap[:'ISO-2022-JP'] = Rubinius::LookupTable.new
Encoding::TranscodingMap[:'ISO-2022-JP'][:'STATELESS-ISO-2022-JP'] = nil
