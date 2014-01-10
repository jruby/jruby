# the versions are declared in ../pom.xml
default_gems = { 
  'rake' => 'rake.version',
  'rdoc' => 'rdoc.version',
  'json' => 'json.version',
  'krypt' => 'krypt.version',
  'krypt-core' => 'krypt.version',
  'krypt-provider-jdk' => 'krypt.version',
  'bouncy-castle-java' => 'bc.version',
  
  # jruby-openssl needs to be at the end due the openssl hack
  # when installing gems
  'jruby-openssl' => 'jopenssl.version'
}

only_specs = ['rdoc', 'json', 'jruby-openssl' ]

project 'JRuby Lib Setup' do

  version = '9000.dev' #File.read( File.join( basedir, '..', 'VERSION' ) )

  model_version '4.0.0'
  id "org.jruby:jruby-lib:#{version}"
  inherit "org.jruby:jruby-parent:#{version}"
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'jruby.home' => '${basedir}/..' )

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}"

  repository( 'http://rubygems-proxy.torquebox.org/releases',
              :id => 'rubygems-releases' )

  # this is not an artifact for maven central
  plugin :deploy, :skip => true 

  # tell maven to download the respective gem artifacts
  default_gems.each do |n,k|
    gem n, "${#{k}}"
  end

  phase :package do
    plugin :dependency do
      items = default_gems.collect do |n,k|
        { 'groupId' =>  'rubygems',
          'artifactId' =>  n,
          'version' =>  "${#{k}}",
          'type' =>  'gem',
          'overWrite' =>  'false',
          'outputDirectory' =>  '${project.build.directory}' }
      end
      execute_goals( 'copy',
                     :id => 'copy gems',
                     'artifactItems' => items )
    end
  end

  execute :install_gems, :package do |ctx|
    require 'fileutils'

    gem_home = File.join( ctx.project.build.directory.to_s, 'rubygems' )
    gems = File.join( gem_home, 'gems' )
    specs = File.join( gem_home, 'specifications' )
    default_specs = File.join( ctx.project.basedir.to_s, 'ruby', 'gems', 'shared', 
                               'specifications', 'default' )
    bin_stubs = File.join( ctx.project.basedir.to_s, 'ruby', 'gems', 'shared', 
                               'gems' )
    shared = File.join( ctx.project.basedir.to_s, 'ruby', 'shared' )
    openssl = File.join( ctx.project.basedir.to_s, 'ruby', 'shared', 'openssl.rb' )
    openssl_copy = File.join( ctx.project.basedir.to_s, 'ruby', 'shared', 'openssl.rb.orig' )

    begin
      # setup a openssl.rb so gems can be installed
      if File.exists?( openssl )
        FileUtils.mv( openssl, openssl_copy )
        File.open( openssl, 'w' )
      end
      
      # now we can require the rubygems staff
      require 'rubygems/installer'

      default_gems.each do |name, key|
        version = ctx.project.properties.get( key )
        
        if Dir[ File.join( specs, "#{name}-#{version}*.gemspec" ) ].empty?
          installer = Gem::Installer.new( File.join( ctx.project.build.directory.to_s, "#{name}-#{version}.gem" ),
                                          :ignore_dependencies => true,
                                          :install_dir => gem_home )
          installer.install 
        
          unless only_specs.include? name
            puts "setup gem #{name}-#{version}"
            Dir[ File.join( gems, "#{name}-#{version}*", 'lib', '*' ) ].each do |f|
              FileUtils.cp_r( f, shared )
            end
          end
      
          bin = File.join( gems, "#{name}-#{version}", 'bin' )
          if File.exists? bin
            Dir[ File.join( bin, '*' ) ].each do |f|
              puts "copy bin-stub #{File.basename( f )}"
              target = File.join( bin_stubs, f.sub( /#{gems}/, '' ) )
              FileUtils.mkdir_p( File.dirname( target ) )
              FileUtils.cp_r( f, target )
            end
          end

          spec = Dir[ File.join( specs, "#{name}-#{version}*.gemspec" ) ].first
          puts "copy specification #{File.basename( spec )}"
          FileUtils.cp( spec, default_specs )
        end
      end
    ensure
      # restore the old openssl.rb if present
      if File.exists?( openssl_copy )
        FileUtils.mv( openssl_copy, openssl )
      end
    end
  end
end
