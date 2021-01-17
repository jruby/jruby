require 'fileutils'
project 'JRuby Windows' do
  
  version = ENV['JRUBY_VERSION'] ||
    File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id "org.jruby:jruby-windows:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'pom'

  # we have no sources and attach an empty jar later in the build to
  # satisfy oss.sonatype.org upload
  plugin( :source, 'skipSource' =>  'true' )

  phase 'package' do
    execute :build_windows_bits do |ctx|
      windows = File.join(ctx.project.build.directory, 'jruby.exe')
      FileUtils.mkdir_p(File.dirname(windows))
      File.write(windows, 'example')
    end

    plugin 'org.codehaus.mojo:build-helper-maven-plugin', '3.0.0' do
      execute_goal :'attach-artifact', artifacts: [{file: 'target/jruby.exe',
                                                    type: 'exe'}]
    end
  end
end

