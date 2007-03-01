require 'digest'

module Digest
  class SHA1 < Digest::Base
  	def digest_name; "SHA1"; end
  	def hex_digits; 40; end
  end
end