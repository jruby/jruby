```
$ git clone https://github.com/lucasocon/openweather.git
RUBYOPT='-I samples/truffle/interop/weather/openweather/lib -I samples/truffle/interop/weather' graalvm/bin/js samples/truffle/interop/weather/weather.js
```

The demo hangs after printing the temperature, as some Ruby service threads
get stuck and aren't shutting down properly.

The Ruby `temperature_in_city` method has a `this` parameter which it wouldn't
normally have, because JS passes `this` as the first argument.
