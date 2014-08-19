#-*- mode: ruby -*-

gemfile

id 'org.rubygems:gem2', '2'

jruby_plugin! :gem, :includeRubygemsInResources => true

properties( 'tesla.dump.pom' => 'pom.xml',
            'jruby.home' => '${basedir}/../../../../../../' )

execute 'FILES.LIST', :'prepare-package' do |ctx|
   require 'jruby/commands'
   JRuby::Commands.files_list( ctx.project.build.output_directory.to_pathname )
end
