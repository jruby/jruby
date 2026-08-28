class SampleBlock
  @@func = nil
  def self.func
    @@func = yield
  end
  func { '1' * 2 }

  def dummy(*args); args.size end
end

{ a:1, b:2 }.each { |k,v| SampleBlock.new.dummy(k, v) }
