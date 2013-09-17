require 'rspec'
require 'openssl'

# This should go into RubySpec, but there are currently no specs
# to cover this class.

describe OpenSSL::SSL::SSLContext do
  subject { OpenSSL::SSL::SSLContext.new }

  if RUBY_VERSION >= "1.9"
    it 'has "ssl_timeout" defined' do
      # "subject.methods.include?" is arguably more preferable,
      # but then we'd have to differentiate by language versions
      lambda { subject.ssl_timeout }.should_not raise_error
    end
  end
end
