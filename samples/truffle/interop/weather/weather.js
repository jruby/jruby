Interop.eval('application/x-ruby', 'require "weather"');

temperature_in_city = Interop.import('temperature_in_city')

console.log('Temperature in New York now: ' + temperature_in_city('New York') + '℃');
