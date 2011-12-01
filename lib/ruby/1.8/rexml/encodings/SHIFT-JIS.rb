module REXML
  module Encoding
    begin
      require 'uconv'
      
      class SHIFT_JISEncoder
        def decode content
          Uconv::sjistou8(content)
        end
        
        def encode(str)
          Uconv::u8tosjis(str)
        end
      end
    rescue LoadError
      require 'nkf'

      SJISTOU8 = '-Swm0x'
      U8TOSJIS = '-Wsm0x'
      
      class SHIFT_JISEncoder
        def decode(str)
          NKF.nkf(SJISTOU8, str)
        end
        
        def encode content
          NKF.nkf(U8TOSJIS, content)
        end
      end
    end

    shift_jis = SHIFT_JISEncoder.new
    b = proc do |obj|
      obj.encoder = shift_jis
    end
    register("SHIFT-JIS", &b)
    register("SHIFT_JIS", &b)
  end
end
