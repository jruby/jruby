if RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/ssl-internal.rb')
else
  load('jopenssl18/openssl/ssl-internal.rb')
end
