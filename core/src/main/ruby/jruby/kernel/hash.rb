class Hash
  def to_proc
    this_hash = self
    proc {|*a| this_hash[*a]}
  end
end