# JRUBY-4334 and JRUBY-4335
describe 'A simple float' do
  it 'roundtrips correctly' do
     [1.5, 4.0/3.0].each { |a|
      ma = Marshal.dump(a)
      b = Marshal.load(ma)
    
      a.should == b
    }
  end
end

describe 'A calculated float' do
  it 'roundtrips correctly' do
    [[1,2,3,4], [81, 2, 118, 3146]].each { |w,x,y,z|
      a = (x.to_f + y.to_f / z.to_f) * Math.exp(w.to_f / (x.to_f + y.to_f / z.to_f))
      ma = Marshal.dump(a)
      b = Marshal.load(ma)
      a.should == b
    }
  end
end

describe 'A long-mantissa float' do
  it 'roundtrips correctly' do
    [
      3.078528197353e+15,
      4.33970549530798e+15,
      4.19333687365058e+15,
      1.43716189322705e+16,
      7.44220139694665e+15].each do |a|
      ma = Marshal.dump(a)
      b = Marshal.load(ma)
      a.should == b
    end
  end
end
