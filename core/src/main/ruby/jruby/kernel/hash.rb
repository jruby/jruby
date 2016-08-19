class Hash
  def to_proc
    method(:[]).to_proc
  end
end