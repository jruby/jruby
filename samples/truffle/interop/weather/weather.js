Interop.eval('application/x-ruby', 'require "weather"');

Weather = Interop.import('weather')

console.log('Temperature in New York now: ' + Weather.temperature_in_city('New York') + '℃');
