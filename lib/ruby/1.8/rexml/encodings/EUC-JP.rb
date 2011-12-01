module REXML
  module Encoding
    begin
      require 'uconv'

      class EUC_JP
        def decode(str)
          Uconv::euctou8(str)
        end
        
        def encode content
          Uconv::u8toeuc(content)
        end
      end
    rescue LoadError
      require 'nkf'

      EUCTOU8 = '-Ewm0'
      U8TOEUC = '-Wem0'

      class EUC_JPEncoder
        def decode(str)
          NKF.nkf(EUCTOU8, str)
        end
      
        def encode content
          NKF.nkf(U8TOEUC, content)
        end
      end
    end

    euc_jp = EUC_JPEncoder.new
    register("EUC-JP") do |obj|
      obj.encoder = euc_jp
    end
  end
end
