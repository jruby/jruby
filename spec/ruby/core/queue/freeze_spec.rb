require_relative '../../spec_helper'

describe "Queue#freeze" do
  ruby_version_is "3.3" do
    it "raises TypeError" do
      -> { Queue.new.freeze }.should raise_error(TypeError)
    end
  end
end
