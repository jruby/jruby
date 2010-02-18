def ant_task(*args, &block)
  @ant ||= Ant.new
  task(*args) do |t|
    @ant.add_target(t.name, &block)
    @ant.execute_target(t.name)
  end
end
