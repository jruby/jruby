require_relative "../fixtures/concrete_subclass_overrides"

# See GH-6718
describe "A Java method from a parent class overridden by its Ruby descendants" do
  it "invokes all overrides followed by the Java method" do
    expect(Child1Override.new.toString).to match(/^child1/)
    expect(Child1NoOverride.new.toString).not_to match(/^child1/)

    expect(Child1OverrideChild2Override.new.toString).to match(/^child2 child1/)
    expect(Child1OverrideChild2NoOverride.new.toString).to match(/^child1/)
    expect(Child1NoOverrideChild2Override.new.toString).to match(/^child2/)
    expect(Child1NoOverrideChild2NoOverride.new.toString).not_to match(/child/)

    expect(Child1OverrideChild2OverrideChild3Override.new.toString).to match(/^child3 child2 child1/)
    expect(Child1OverrideChild2NoOverrideChild3Override.new.toString).to match(/^child3 child1/)
    expect(Child1NoOverrideChild2OverrideChild3Override.new.toString).to match(/^child3 child2/)
    expect(Child1NoOverrideChild2NoOverrideChild3Override.new.toString).to match(/child3/)
    expect(Child1OverrideChild2OverrideChild3NoOverride.new.toString).to match(/^child2 child1/)
    expect(Child1OverrideChild2NoOverrideChild3NoOverride.new.toString).to match(/^child1/)
    expect(Child1NoOverrideChild2OverrideChild3NoOverride.new.toString).to match(/^child2/)
    expect(Child1NoOverrideChild2NoOverrideChild3NoOverride.new.toString).not_to match(/child/)
  end
end