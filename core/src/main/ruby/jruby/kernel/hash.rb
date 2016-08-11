class Hash
  def to_proc
    hash_self = self
    ->(key) {hash_self[key]}
  end
end