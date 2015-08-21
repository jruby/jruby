require 'rspec'
require 'stringio'

unless RUBY_VERSION =~ /1\.8/
  describe 'StringIO.set_encoding' do
    it 'does not change the encoding of shared empty bytelist' do
      StringIO.new.set_encoding('UTF-16LE')
      empty_utf8 = ''
      utf8_matches = empty_utf8.scan(/(.*)/)[0]
      expect(utf8_matches[0].encoding.to_s).not_to eq('UTF-16LE')
    end
  end
end
