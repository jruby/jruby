require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/fiber/resume', __FILE__)

with_feature :fiber do
  describe "Fiber#resume" do
    it_behaves_like :fiber_resume, :resume
  end

  describe "Fiber#resume" do
    it "returns control to the calling Fiber if called from one" do
      fiber1 = Fiber.new { :fiber1 }
      fiber2 = Fiber.new { fiber1.resume; :fiber2 }
      fiber2.resume.should == :fiber2
    end

    with_feature :fork do
      ruby_bug "redmine #595", "3.0.0" do
        it "executes the ensure clause" do
          rd, wr = IO.pipe
          if Kernel::fork then
            wr.close
            rd.read.should == "executed"
            rd.close
          else
            rd.close
            Fiber.new {
              begin
                Fiber.yield
              ensure
                wr.write "executed"
              end
            }.resume
            exit 0
          end
        end
      end
    end
  end
end
