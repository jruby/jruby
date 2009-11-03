namespace :maven do
  class Pom
    def initialize(filename)
      @filename = filename
      @lines = IO.readlines(filename)
    end

    def update_version(version)
      group = nil
      artifact = nil
      @lines.each do |line|
        match = line.match(%r{<groupId>([^<]+)</groupId>})
        if match
          group = match[1]
          next
        end
        match = line.match(%r{<artifactId>([^<]+)</artifactId>})
        if match
          artifact = match[1]
          next
        end
        if line =~ %r{<version>[0-9][^<]+</version>} && group =~ /^org.jruby/ && artifact =~ /^(jruby|shared)/
          line.sub!(/<version>([^<]+)<\/version>/, "<version>#{version}</version>")
        end
      end
    end

    def save
      File.open(@filename, 'w') {|f| @lines.each {|l| f << l } }
    end
  end

  desc "Update versions in maven poms with string passed in ENV['VERSION']"
  task :updatepoms do
    version = ENV['VERSION'] or abort("Pass the new version with VERSION={version}")
    dir =  Dir.pwd
    Dir["#{dir}/**/pom.xml"].each do |f|
      puts "updating #{f}"
      pom = Pom.new(f)
      pom.update_version(version)
      pom.save
    end
  end
end
