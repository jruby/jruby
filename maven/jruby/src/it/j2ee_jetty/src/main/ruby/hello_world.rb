require 'openssl'

class HelloWorld < String
  def initialize
    super "hello world: #{OpenSSL::Random.random_bytes( 16 )}"
  end
end
