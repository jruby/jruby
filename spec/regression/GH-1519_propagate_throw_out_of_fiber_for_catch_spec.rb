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

  it "propagates exceptions out to waiting fiber/thread" do
    begin
      Fiber.new do
        Fiber.new do
          raise
        end.resume
      end.resume
    rescue Exception => e
    end

    expect(e).not_to eq nil
    expect(e.class).to eq RuntimeError
  end
end unless RUBY_VERSION < '1.9'
