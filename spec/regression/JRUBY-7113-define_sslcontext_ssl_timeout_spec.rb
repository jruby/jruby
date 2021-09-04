require 'rspec'
require 'openssl'

# This should go into RubySpec, but there are currently no specs
# to cover this class.

describe OpenSSL::SSL::SSLContext do
  subject { OpenSSL::SSL::SSLContext.new }

  it 'has "ssl_timeout" defined' do
    # "subject.methods.include?" is arguably more preferable,
    # but then we'd have to differentiate by language versions
    expect { subject.ssl_timeout }.not_to raise_error
  end
end
