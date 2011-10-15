module Process
  def self.spawn(*args)
    _spawn_internal(*JRuby::ProcessUtil.exec_args(args))
  end
end
