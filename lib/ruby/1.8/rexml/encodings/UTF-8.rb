module REXML
  module Encoding
    class UTF_8Encoder
      def encode content
        content
      end
      
      def decode(str)
        str
      end
    end

    utf_8 = UTF_8Encoder.new
    register(UTF_8) do |obj|
      obj.encoder = utf_8
    end
  end
end
