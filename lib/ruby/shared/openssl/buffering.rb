if RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/buffering.rb')
else
  load('jopenssl18/openssl/buffering.rb')
end
