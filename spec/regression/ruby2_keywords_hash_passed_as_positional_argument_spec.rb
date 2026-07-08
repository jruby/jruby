# A ruby2_keywords-flagged Hash passed as an ordinary positional argument
# (no splat, no keywords at the call site) must arrive in the callee as the
# same object with its flag intact. In 9.4.15.0 the JIT specific-arity
# receive path dup'ed and unflagged it, which corrupted Rails ActiveJob
# argument serialization (Hash.ruby2_keywords_hash? returned false inside
# ActiveJob::Arguments.serialize_argument, so kwargs were persisted without
# the _aj_ruby2_keywords marker).
describe "A ruby2_keywords-flagged Hash passed as a positional argument" do
  def probe(hash)
    [hash, Hash.ruby2_keywords_hash?(hash)]
  end

  it "is passed through without being copied or unflagged" do
    flagged = Hash.ruby2_keywords_hash({ a: 1 })
    failure = nil

    # Loop enough for the method to be JIT compiled; the interpreter was
    # never affected.
    10_000.times do |i|
      received, marked = probe(flagged)
      unless received.equal?(flagged) && marked
        failure = "iteration #{i}: same_object=#{received.equal?(flagged)}, flagged=#{marked}"
        break
      end
    end

    expect(failure).to be_nil
  end
end
