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
  end
end
