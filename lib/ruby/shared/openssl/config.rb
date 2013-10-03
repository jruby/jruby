if RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/config.rb')
else
  load('jopenssl18/openssl/config.rb')
end
