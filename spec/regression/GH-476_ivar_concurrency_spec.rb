require 'rspec'

describe 'Accessing instance variables' do
  it 'should not lose concurrent writes under growth operations' do
    lost_writes = false  
  
    (0..1000).each do |i|
      clazz = Class.new
      object = clazz.new
      
      mutate = true
      
      # probing thread
      t1 = Thread.new do
        (0..10000).each do |i|
          object.instance_variable_set(:@foo, i)
          lost_writes = true if object.instance_variable_get(:@foo) != i
        end
        
      end
      
      # mutating thread
      t2 = Thread.new{(0..10000).each{break unless mutate;object.instance_variable_set(:"@bar_#{rand(100000)}",1)}}
      
      t1.join
      mutate = false
      t2.join
      
      break if lost_writes
    end
    
    lost_writes.should be_false
  end
end
