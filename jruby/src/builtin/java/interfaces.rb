JavaUtilities.extend_proxy('java.lang.Runnable') {
  def to_proc
    proc { self.run }
  end
}

