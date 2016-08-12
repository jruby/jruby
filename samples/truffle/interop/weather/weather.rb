require 'openweather2'

Openweather2.configure do |config|
  # This is the endpoint and API key from the tests in the Openweather2 gem
  config.endpoint = 'http://api.openweathermap.org/data/2.5/weather'
  config.apikey = 'dd7073d18e3085d0300b6678615d904d'
end

module Weather
  def self.temperature_in_city(name)
    name = Truffle::Interop.from_java_string(name)
    weather = Openweather2.get_weather(city: name, units: 'metric')
    weather.temperature
  end
end

Truffle::Interop.export :weather, Weather
