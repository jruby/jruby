require 'rbconfig'

module JRuby
  class Commands
    class << self
      dollar_zero = Config::CONFIG['bindir'] + "/#{$0}"
      # Provide method aliases to scripts commonly found in ${jruby.home}/bin.
      ruby_bin = File.open(dollar_zero) {|io| (io.readline rescue "") =~ /^#!.*ruby/} rescue nil
      if ruby_bin
        define_method File.basename(dollar_zero) do
          $0 = dollar_zero
          load dollar_zero
        end
      end

      def maybe_install_gems
        require 'rubygems'
        require 'rubygems/format'

        # Want the kernel gem method here; expose a backdoor b/c RubyGems 1.3.1 made it private
        Object.class_eval { def __gem(g); gem(g); end }
        gem_loader = Object.new

        ARGV.delete_if do |g|
          # skip options
          next false if g =~ /^-/

          if File.exist?(g) # local gem
            begin
              gem = Gem::Format.from_file_by_path(g)
              name = gem.spec.name
              ver = gem.spec.version
              dep = Gem::Dependency.new(name, ver)

              # check, whether the same gem is already installed
              if Gem.source_index.search(dep).empty?
                false
              else
                puts "#{g} already installed"
                true
              end
            rescue Gem::Exception
              false
            end
          else
            # remote gem
            begin
              gem_loader.__gem(g)
              puts "#{g} already installed"
              true
            rescue Gem::LoadError
              false
            end
          end
        end

        Object.class_eval { remove_method :__gem }

        unless ARGV.reject{|a| a =~ /^-/}.empty?
          ARGV.unshift "install"
          begin
            load Config::CONFIG['bindir'] + "/gem"
          rescue SystemExit => e
            # don't exit in case of 0 return value from 'gem'
            exit(e.status) unless e.success?
          end
        end
        generate_bat_stubs
      end

      def generate_bat_stubs
        Dir[Config::CONFIG['bindir'] + '/*'].each do |fn|
          next unless File.file?(fn)
          next if fn =~ /.bat$/
          next if File.exist?("#{fn}.bat")
          next unless File.open(fn) {|io| (io.readline rescue "") =~ /^#!.*ruby/}
          puts "Generating #{File.basename(fn)}.bat"
          # FIXME: Outdated BAT template, update!
          File.open("#{fn}.bat", "wb") do |f|
            f << "@echo off\n"
            f << "call \"%~dp0jruby\" -S #{File.basename(fn)} %*\n"
          end
          # FIXME: foo.bat? Really? There is no foo.bat anywhere.
        end if File.writable?(File.join(Config::CONFIG['bindir'], 'foo.bat'))
      end

      def method_missing(name, *)
        $stderr.puts "jruby: No such file, directory, or command -- #{name}"
      end
    end
  end
end

