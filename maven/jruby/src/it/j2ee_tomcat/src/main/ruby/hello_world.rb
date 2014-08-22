require 'openssl'
require 'base64'

class HelloWorld < String
  def initialize
    super "hello world: #{Base64.encode64( OpenSSL::Random.random_bytes( 16 ) )}"
  end
end
