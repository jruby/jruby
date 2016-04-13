module HashyKwargs

  DEFAULT_ARGS = { 'str' => 1, sym: 2 }
  private_constant :DEFAULT_ARGS

  def generic(*args, **kwargs, &block)
    [ args, kwargs, block ]
  end

  def self.kwargs1(sym: 1, sec: 2)
    sym || DEFAULT_ARGS[:sym]
  end

  def self.kwargs2(req:, **opts); [ req, opts ] end

  DEFAULT_ARGS['foo'] || DEFAULT_ARGS['str']

  Hash.new.tap do |hash|
    hash['one'] = 11 / 10; hash['two'] = 22 / 10
    @@hash_one = hash['tri'] || hash['one']
  end

end