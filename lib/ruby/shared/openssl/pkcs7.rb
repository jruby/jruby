if RUBY_VERSION >= '1.9.0'
  raise LoadError, "no such library in 1.9 mode: openssl/pkcs7"
else
  load('jopenssl18/openssl/pkcs7.rb')
end
