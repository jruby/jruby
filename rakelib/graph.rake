namespace :graph do
  def sanitize(name)
    name.gsub(/[-.]/, '_')
  end

  task :viz do
    require 'tempfile'
    ant_import # load build.xml if not already loaded
    f = Tempfile.open "build_graph"
    begin
      f.puts "digraph ant {"
      ant.project.targets.each do |name, target|
        target.dependencies.to_a.each do |dep|
          f.puts "#{sanitize(dep)} -> #{sanitize(name)}"
        end
      end
      f.puts "}"
    ensure
      f.close
    end
    system "cat #{f.path} | dot -Tpng -x > build_graph.png"
    f.delete
  end
end
