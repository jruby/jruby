require 'rspec'

if RUBY_VERSION > '1.9'
  describe 'Date#zone' do
    subject { Time.new(2013,4,22,14,18,05,'-05:00') }

    it 'returns nil' do
      subject.zone.should be_nil
    end

  end
end