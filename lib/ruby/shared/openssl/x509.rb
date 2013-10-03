if RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/x509.rb')
else
  load('jopenssl18/openssl/x509.rb')
end
