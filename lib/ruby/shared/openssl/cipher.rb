if RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/cipher.rb')
else
  load('jopenssl18/openssl/cipher.rb')
end
