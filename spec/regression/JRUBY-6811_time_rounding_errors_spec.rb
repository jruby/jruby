require 'rspec'

if RUBY_VERSION > '1.9'
  describe 'Time.at with float' do
    it 'preserves sub-ms part when passing through getutc' do
      time = Time.at(1234441536.123456)
      utc = time.getutc
      utc.to_f.to_s.should == '1234441536.123456'
    end

    it 'preserves sub-ms part when passing through localtime' do
      time = Time.at(1234441536.123456)
      local = time.localtime
      local.to_f.to_s.should == '1234441536.123456'
    end
  end
end