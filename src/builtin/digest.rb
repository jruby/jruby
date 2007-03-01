require 'java'

module Digest
  class Base
    include_class 'java.security.MessageDigest'
    include_class 'java.lang.String'
    include_class 'java.math.BigInteger'
    
  	def initialize(content=nil)
  	  @md = MessageDigest.getInstance(digest_name)
	  update(content) unless content.nil?
  	end
  	
  	def <<(content)
  	  @md.update(String.new(content).getBytes)
  	end

	alias_method :update, :<<

	def digest
	  @md.clone.digest.to_a.pack('c*')
	end
	    
    def hexdigest
	  digest = BigInteger.new(1, @md.clone.digest).toString(16)
	  
	  ("0" * (hex_digits - digest.size)) << digest
    end

	def ==(other)
	  hexdigest == other.hexdigest
	end    

  	def Base.digest(content)
  	  new(content).digest
  	end
  	
  	def Base.hexdigest(content)
  	  new(content).hexdigest
  	end
  end
end