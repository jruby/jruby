# encoding: UTF-8
require 'rspec'

describe 'JRUBY-6510: String.encode!' do
  it 'takes a hash argument' do
    enc_dft_in = Encoding.default_internal
    begin
      Encoding.default_internal = 'ISO-8859-1'
      s = "äöü"
      s.encode!({:invalid => :replace, :undef => :replace})
      s.encoding.name.should == 'ISO-8859-1'
    ensure
      Encoding.default_internal = enc_dft_in unless enc_dft_in.nil?
    end
  end
end
