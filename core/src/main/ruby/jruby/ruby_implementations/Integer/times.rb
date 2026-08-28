class Integer
  def times
    i = 0
    while i < self do
      yield i
      i += 1
    end
  end
end
