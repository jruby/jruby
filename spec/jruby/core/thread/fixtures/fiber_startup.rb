require 'java'
require 'rspec/autorun'

virtual = ARGV.shift == 'true'

RSpec.describe "Fiber startup with thread.vthread=#{virtual}" do
  it 'initializes fibers concurrently without starving their backing threads' do
    # More creators than carriers exposes competition between initialization
    # and the new fiber's adoption. Check results instead of scheduling speed.
    threads = Array.new(64) do
      Thread.new do
        expect(java.lang.Thread.current_thread.virtual?).to eq(virtual)
        Array.new(32) { Fiber.new { :finished }.resume }
      end
    end
    expect(threads.map(&:value)).to all(eq([:finished] * 32))
  end
end
