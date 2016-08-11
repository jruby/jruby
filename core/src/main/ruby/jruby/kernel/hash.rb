class Hash
  def to_proc
    hash_self = self
    proc {|key| hash_self[key]}
  end
end