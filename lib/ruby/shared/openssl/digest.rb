if RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/digest.rb')
else
  load('jopenssl18/openssl/digest.rb')
end
