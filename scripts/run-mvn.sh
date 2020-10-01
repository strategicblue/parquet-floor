#!/bin/sh -eu

if [ -z ${JAVA_VERSION+x} ]; then
  echo "JAVA_VERSION is unset, suggesting you are not running in CI, which is bad because this script will hurt your local machine" 1>&2
  exit 1
fi

ROOT_DIR="$( cd "$( dirname "${0}" )/.." >/dev/null && pwd -P )"

rm -rf "${HOME}/.m2"
mkdir -p "${ROOT_DIR}/../maven/repository"
ln -s "$(readlink -f "${ROOT_DIR}/../maven")" "${HOME}/.m2"
cp "${ROOT_DIR}/settings.xml" "${HOME}/.m2/."

mvn "${@}"