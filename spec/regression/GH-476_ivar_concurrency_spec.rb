require 'rspec'

describe 'Accessing instance variables' do
  it 'should not lose concurrent writes under growth operations' do
    (0..1000).each do |i|
      clazz = Class.new
      object = clazz.new
      
      # mutating thread
      t = Thread.new do
        Thread.pass # try to let probe code below start first
        100.times do |i|
          object.instance_variable_set(:"@bar#{i}",1)
        end
      end

      # probing logic
      (0..100).each do |i|
        object.instance_variable_set(:"@foo#{i}", i)
        read_value = object.instance_variable_get(:"@foo#{i}")
        expect(read_value).to eq(i)
      end

      # cleanup
      t.join
    end
  end
end
