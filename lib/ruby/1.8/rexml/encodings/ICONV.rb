require "iconv"
raise LoadError unless defined? Iconv

module REXML
  module Encoding
    class ICONVEncoder
      def decode(str)
        Iconv.conv(UTF_8, @encoding, str)
      end
      
      def encode(content)
        Iconv.conv(@encoding, UTF_8, content)
      end
    end

    iconv = ICONVEncoder.new
    register("ICONV") do |obj|
      Iconv.conv(UTF_8, obj.encoding, nil)
      obj.encoder = iconv
    end
  end
end
