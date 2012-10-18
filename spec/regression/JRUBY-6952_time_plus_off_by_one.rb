describe 'JRUBY-6952: Time#+ gives off-by-one error with fractional microseconds' do
  if RUBY_VERSION >= "1.9"
    it 'does sane arithmetic' do
      t = Time.new(2012, 1, 1, 23, 59, 59, 999999.9)
      t_inc = t + 1
      diff = t_inc.to_i - t.to_i
      diff.should == 1
    end
  end
end
