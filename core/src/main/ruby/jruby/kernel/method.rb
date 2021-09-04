class Method
  # From https://github.com/marcandre/backports, MIT license
  def <<(g)
    to_proc << g
  end

  # From https://github.com/marcandre/backports, MIT license
  def >>(g)
    to_proc >> g
  end
end