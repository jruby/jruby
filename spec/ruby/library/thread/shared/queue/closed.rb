describe :queue_closed?, shared: true do
  it "returns false initially" do
    queue = @object
    queue.closed?.should be_false
  end

  it "returns true when the queue is closed" do
    queue = @object
    queue.close
    queue.closed?.should be_true
  end
end
