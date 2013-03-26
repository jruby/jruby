require 'rspec'

if RUBY_VERSION > '1.9'
  describe 'Time#+' do
    it 'does not introduce rounding errors' do
      time = Time.new(2012, 10, 17, 23, 59, 59, 86399.9)
      time_p1 = time + 1
      time_p1_m1 = time_p1 - 1

      (time_p1.to_i - time.to_i).should == 1
      time_p1_m1.to_i.should == time.to_i
    end
  end
end
