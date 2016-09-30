PORT=14873

function test_server {
  serverpid=$!
  local cmd="curl -s http://localhost:$PORT/"
  local response=""
  while ! response=$($cmd) || [[ -z response ]];
  do
    sleep 1
  done
  jobs -l
  pkill -P $serverpid
  wait $serverpid || true
  if [[ $response != *"Hello"* ]]
  then
    echo Response not expected:
    echo $response
    exit 1
  fi
}

function jt {
  ruby tool/jt.rb "$@"
}
