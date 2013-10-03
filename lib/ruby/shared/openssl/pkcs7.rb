if RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/pkcs7.rb')
else
  raise LoadError, "no such library in 1.8 mode: openssl/pkcs7"
end
