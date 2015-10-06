require File.expand_path('../boot', __FILE__)

require 'rails/all'

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

require 'leafy/metrics'
require 'leafy/health'
require 'leafy/instrumented/instrumented'
require 'leafy/instrumented/collected_instrumented'
require 'leafy/rack/admin'
require 'leafy/rack/instrumented'


module Myapp
  class Application < Rails::Application
    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration should go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded.

    # Set Time.zone default to the specified zone and make Active Record auto-convert to this zone.
    # Run "rake -D time" for a list of tasks for finding time zone names. Default is UTC.
    # config.time_zone = 'Central Time (US & Canada)'

    # The default locale is :en and all translations from config/locales/*.rb,yml are auto loaded.
    # config.i18n.load_path += Dir[Rails.root.join('my', 'locales', '*.{rb,yml}').to_s]
    # config.i18n.default_locale = :de

    # Do not swallow errors in after_commit/after_rollback callbacks.
    config.active_record.raise_in_transactional_callbacks = true

    metrics = Leafy::Metrics::Registry.new
    health = Leafy::Health::Registry.new

    config.middleware.use( Leafy::Rack::Admin, metrics, health )
    config.middleware.use( Leafy::Rack::Metrics, metrics )
    config.middleware.use( Leafy::Rack::Health, health )
    config.middleware.use( Leafy::Rack::Ping )
    config.middleware.use( Leafy::Rack::ThreadDump )
    config.middleware.use( Leafy::Rack::Instrumented, Leafy::Instrumented::Instrumented.new( metrics, 'webapp' ) )
    config.middleware.use( Leafy::Rack::Instrumented, Leafy::Instrumented::CollectedInstrumented.new( metrics, 'collected' ) )

    config.data = OpenStruct.new
    config.data.surname = 'meier'
    config.data.firstname = 'christian'

    metrics.register_gauge('app.data_length' ) do
      Myapp::Application.config.data.surname.length + Myapp::Application.config.data.firstname.length
    end
    
    health.register( 'app.health' ) do
      if Myapp::Application.config.data.surname.length + Myapp::Application.config.data.firstname.length < 4
        "stored names are too short"
      end
    end

    config.histogram = metrics.register_histogram( 'app.name_length' )
  end
end
