require 'rspec'

describe "A newly created thread" do
  it "should always appear in Thread.list" do
    threads = []

    100.times do
      threads << Thread.new do
        1000.times do 
          objects = Thread.list.map{|t|t.object_id}
          objects.should include(Thread.current.object_id)
        end
      end
    end

    threads.each do |t| 
      t.join
    end
  end
end
