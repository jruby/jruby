describe "A Fiber" do
  it "propagates throw out to waiting fiber/thread's catch" do
    result = catch(:foo) do
      Fiber.new do
        Fiber.new do
          throw :foo, "hooray"
        end.resume
      end.resume
    end

    expect(result).to eq("hooray")
  end
end unless RUBY_VERSION < '1.9'