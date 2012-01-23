#! /bin/sh

# see git_bisect_run_general.sh for more explanation
exec `dirname $0`/git_bisect_run_general.sh -c "spec/mspec/bin/mspec $*"