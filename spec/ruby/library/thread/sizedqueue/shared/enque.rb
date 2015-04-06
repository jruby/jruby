describe :sizedqueue_enq, :shared => true do
  it "blocks if queued elements exceed size" do
    q = @object.new(1)

    q.size.should == 0
    q.send(@method, :first_element)
    q.size.should == 1

    blocked_thread = Thread.new { q.send(@method, :second_element) }
    sleep 0.01 until blocked_thread.stop?

    q.size.should == 1
    q.pop.should == :first_element
    q.size.should == 0

    blocked_thread.join
    q.size.should == 1
    q.pop.should == :second_element
    q.size.should == 0
  end

  ruby_version_is "2.2" do
    it "raises a ThreadError if queued elements exceed size when not blocking" do
      q = @object.new(2)
      method = @method

      non_blocking = true
      add_to_queue = lambda { q.send(method, Object.new, non_blocking) }

      q.size.should == 0
      add_to_queue.call
      q.size.should == 1
      add_to_queue.call
      q.size.should == 2
      add_to_queue.should raise_error(ThreadError)
    end
  end
end
