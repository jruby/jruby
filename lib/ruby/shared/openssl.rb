begin
  require 'jopenssl/load'
rescue LoadError => e
  e.message.sub! 'jopenssl/load', 'openssl'
end
