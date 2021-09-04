require 'cuba'
require "cuba/render"
require 'cuba/safe'
require 'json'
require 'ostruct'
require 'leafy/metrics'
require 'leafy/health'
require 'leafy/instrumented/instrumented'
require 'leafy/instrumented/collected_instrumented'
require 'leafy/rack/admin'
require 'leafy/rack/instrumented'

data = OpenStruct.new
data.surname = 'meier'
data.firstname = 'christian'

Cuba.plugin Cuba::Safe
Cuba.plugin Cuba::Render
Cuba.settings[:render][:views] = "./app/views"

begin
  metrics = Leafy::Metrics::Registry.new
  health = Leafy::Health::Registry.new

  Cuba.use( Leafy::Rack::Admin, metrics, health )
  Cuba.use( Leafy::Rack::Metrics, metrics )
  Cuba.use( Leafy::Rack::Health, health )
  Cuba.use( Leafy::Rack::Ping )
  Cuba.use( Leafy::Rack::ThreadDump )
  Cuba.use( Leafy::Rack::Instrumented, Leafy::Instrumented::Instrumented.new( metrics, 'webapp' ) )
  Cuba.use( Leafy::Rack::Instrumented, Leafy::Instrumented::CollectedInstrumented.new( metrics, 'collected' ) )

  metrics.register_gauge('app.data_length' ) do
    data.surname.length + data.firstname.length
  end

  health.register( 'app.health' ) do
    if data.surname.length + data.firstname.length < 4
      "stored names are too short"
    end
  end

  Cuba.settings[:histogram] = metrics.register_histogram( 'app.name_length' )
end

Cuba.define do
  on get do
    on 'shutdown' do
      java.lang.Runtime.runtime.exit 0
    end

    on 'app' do
      p @person = data
      render('person')
    end

    on 'person' do
      p @person = data
      content_type 'application/json'
      res.write( { :surname =>  data.surname, :firstname => data.firstname }.to_json )
    end
  end
  
  on env["REQUEST_METHOD"] == "PATCH", 'person' do
    payload = JSON.parse request.body.read
    data.send :"#{payload.keys.first}=", payload.values.first
    Cuba.settings[:histogram].update( data.surname.length + data.firstname.length )
    res.status 205
  end
end
