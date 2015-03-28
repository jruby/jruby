describe "Cross-platform compatibility" do
  it "defines RUBY_ENGINE" do
    expect(defined?(RUBY_ENGINE)).to_not eq nil
  end

  it "defines RUBY_ENGINE_VERSION" do
    expect(defined?(RUBY_ENGINE_VERSION)).to_not eq nil
  end
end
