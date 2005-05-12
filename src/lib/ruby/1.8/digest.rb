require 'java'

module Digest
  class Base
    include_class 'java.security.MessageDigest'
    include_class 'java.lang.String'
    include_class 'java.math.BigInteger'
    
  	def initialize
  	  @md = MessageDigest.getInstance(digest_name)
  	end
  	
  	def <<(content)
  	  @md.update(String.new(content).getBytes)
  	end

	alias_method :update, :<<

	def digest
	  @md.digest.to_a.pack('c*')
	end
	    
    def hexdigest
	  digest = BigInteger.new(1, @md.digest).toString(16)
	  
	  ("0" * (hex_digits - digest.size)) << digest
    end
    
  	def Base.digest(content)
  	  md = new
  	  md.update(content)
  	  md.digest
  	end
  	
  	def Base.hexdigest(content)
  	  md = new
  	  md.update(content)
  	  md.hexdigest
  	end
  end
end