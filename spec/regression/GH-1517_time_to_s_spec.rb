require 'rspec'

# https://github.com/jruby/jruby/issues/1517
if RUBY_VERSION > '1.9'
  describe 'Time#to_s' do
    it 'returns the same string' do
      t1 = Time.new(2014, 1, 2, 3, 4, 5, '+05:00')
      t2 = Time.new(2014, 1, 2, 3, 4, 5, 18000)
      
      t1.to_s.should == '2014-01-02 03:04:05 +0500'
      t1.to_s.should == t2.to_s
    end
  end
  
  describe 'Time#eql?' do
    it 'returns true' do
      t1 = Time.new(2014, 1, 2, 3, 4, 5, '+05:00')
      t2 = Time.new(2014, 1, 2, 3, 4, 5, 18000)
      
      t1.eql?(t2).should == true
    end
  end
  
  describe 'Time#zone' do
    it 'returns nil' do
      t1 = Time.new(2014, 1, 2, 3, 4, 5, '+05:00')
      
      # in ruby 1.9.3, t1.zone return nil 
      t1.zone.should == nil
    end
  end
  
  describe 'Time#utc_offset' do
    it 'returns collect value' do
      t1 = Time.new(2014, 1, 2, 3, 4, 5, '+05:00')
      
      # in ruby 1.9.3, t1.utc_offset return 18000
      t1.utc_offset.should == 18000
    end
  end
  
end