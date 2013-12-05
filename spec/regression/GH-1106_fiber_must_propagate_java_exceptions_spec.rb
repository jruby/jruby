# GH-1106: Java exceptions raised in fibers cause the fiber to die without notifying parent

describe "A Fiber" do
  describe "that sees a Java exception raised all the way out of its body" do
    it "propagates that exception to any resuming thread" do
      f = Fiber.new { raise java.lang.Exception.new }

      lambda do
        f.resume
      end.should raise_error(java.lang.Exception)
    end

    it "shuts down its internal queue so no further resumes are possible" do
      f = Fiber.new { raise java.lang.Exception.new }

      begin 
        f.resume
      rescue java.lang.Exception
      end

      lambda do
        f.resume
      end.should raise_error(FiberError)
    end
  end
  
  describe "that is killed like a thread" do
    it "kills its parent thread" do
      go = false
      t = Thread.new do
        Thread.pass until go
        Fiber.new { Thread.exit }.resume
      end
      
      Thread.pass until t.status
      
      go = true
      
      Thread.pass while t.status
      
      t.status.should == false # dead
    end
  end
end if RUBY_VERSION >= "1.9"
