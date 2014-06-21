require 'rspec'

top_self = self

class C; end
C.class_eval {}

top_binding = binding

describe "class_eval" do
  it "should not clobber self in previous frame" do
     eval("self", top_binding).should == top_self
  end
end
