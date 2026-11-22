require 'rspec'
require 'objspace'

# lib/ruby/stdlib/objspace.rb sets up allocation tracing through JRuby::Util.load_ext. Loading the file again
# while tracing must not lose what was recorded, stop tracing, or keep tracing after the last stop.
describe "ObjectSpace.trace_object_allocations_start" do
  it "keeps tracing and what it recorded when objspace.rb is loaded again" do
    ObjectSpace.trace_object_allocations_start
    begin
      before_load = Object.new; before_line = __LINE__
      load 'objspace.rb'
      after_load = Object.new; after_line = __LINE__

      expect(ObjectSpace.allocation_sourceline(before_load)).to eq(before_line)
      expect(ObjectSpace.allocation_sourceline(after_load)).to eq(after_line)
    ensure
      ObjectSpace.trace_object_allocations_stop
    end

    expect(ObjectSpace.allocation_sourcefile(Object.new)).to be_nil
  end
end
