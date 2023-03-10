#!/bin/sh -eu

if [ -z ${JAVA_VERSION+x} ]; then
  echo "JAVA_VERSION is unset, suggesting you are not running in CI, which is bad because this script will hurt your local machine" 1>&2
  exit 1
fi

ROOT_DIR="$( cd "$( dirname "${0}" )/.." >/dev/null && pwd -P )"

# skip release if last commit was a release
git -C "${ROOT_DIR}" log -n 1 --format=%an | grep -e "${GIT_AUTHOR_NAME}" && exit 0

sed -i 's/-SNAPSHOT//' "${ROOT_DIR}/pom.xml"

echo "${GPG_PRIVATE_KEY}" | gpg --batch --import

"${ROOT_DIR}/scripts/run-mvn.sh" deploy release:update-versions -P release -Dmaven.test.skip=true

git -C "${ROOT_DIR}" add 'pom.xml'
git -C "${ROOT_DIR}" commit -m 'released version [skip ci]'