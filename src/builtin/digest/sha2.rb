require 'digest'

module Digest
  class SHA256 < Digest::Base
  	def digest_name; "SHA-256"; end
  	def hex_digits; 64; end
  end
  class SHA384 < Digest::Base
  	def digest_name; "SHA-384"; end
  	def hex_digits; 96; end
  end
  class SHA512 < Digest::Base
  	def digest_name; "SHA-512"; end
  	def hex_digits; 128; end
  end
end