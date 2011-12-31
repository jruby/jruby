module REXML
  module Encoding
    class US_ASCIIEncoder
      # Convert from UTF-8
      def encode content
        array_utf8 = content.unpack('U*')
        array_enc = []
        array_utf8.each do |num|
          if num <= 0x7F
            array_enc << num
          else
            # Numeric entity (&#nnnn;); shard by  Stefan Scholl
            array_enc.concat "&\##{num};".unpack('C*')
          end
        end
        array_enc.pack('C*')
      end
      
      # Convert to UTF-8
      def decode(str)
        str.unpack('C*').pack('U*')
      end
    end

    us_ascii = US_ASCIIEncoder.new
    register("US-ASCII") do |obj|
      us_ascii.encoder = us_ascii
    end
  end
end
