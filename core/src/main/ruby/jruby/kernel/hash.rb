class Hash
  def to_proc
    ->(key) {self[key]}
  end
end