require 'digest'

module Digest
  class MD5 < Digest::Base
  	def digest_name; "MD5"; end
  	def hex_digits; 32; end
  end
end