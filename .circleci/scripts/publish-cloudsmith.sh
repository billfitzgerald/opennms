#!/bin/bash

set -e
set -o pipefail

MYDIR="$(dirname "$0")"
MYDIR="$(cd "$MYDIR" || exit 1; pwd)"

PROJECT="opennms"
REPO=""
VERSION="$("${MYDIR}/pom2version.sh" "${MYDIR}/../../pom.xml")"

case "${CIRCLE_BRANCH}" in
  release-*.x)
    YEAR="$(echo "${CIRCLE_BRANCH}" | sed -e 's,^release-,,' -e 's,.x$,,')"
    if [[ "${YEAR}" =~ ^[[:digit:]]+$ ]]; then
      REPO="meridian-${YEAR}-testing"
    else
      echo "unable to determine associated Meridian release for branch ${CIRCLE_BRANCH}"
      exit 0
    fi
    ;;
  master-*)
    # shellcheck disable=SC2001
    YEAR="$(echo "${CIRCLE_BRANCH}" | sed -e 's,^master-,,')"
    if [[ "${YEAR}" =~ ^[[:digit:]]+$ ]]; then
      REPO="meridian-${YEAR}"
    else
      echo "unable to determine associated Meridian release for branch ${CIRCLE_BRANCH}"
      exit 0
    fi
    ;;
  *)
    echo "This branch is not eligible for deployment: ${CIRCLE_BRANCH}"
    exit 0
    ;;
esac

if [ -z "$REPO" ]; then
  echo "unable to determine the correct repo for branch ${CIRCLE_BRANCH}"
  exit 1
fi

publishPackage() {
  local _tmpdir;
  _tmpdir="$(mktemp -d 2>/dev/null || mktemp -d -t 'publish_cloudsmith_')"
  echo "publishing:" "$@"
  "$@" >"${_tmpdir}/publish.log" 2>&1
  ret="$?"
  cat "${_tmpdir}/publish.log"
  if [ "$(grep -c "This package duplicates the attributes of another package" < "${_tmpdir}/publish.log")" -gt 0 ]; then
    echo "Duplicate upload... skipping."
    return 0
  fi
  rm "${_tmpdir}/publish.log"
  rmdir "${_tmpdir}" || :
  return "$ret"
}

publishPackage cloudsmith push raw \
  --republish \
  --version "${VERSION}" \
  --name "${REPO}/minion-config-schema.yml" \
  --description "minion-config-schema.yml for version ${VERSION} in the ${REPO} repository" \
  "${PROJECT}/config-schema" \
  "/tmp/minion-config-schema/minion-config-schema.yml"

for FILE in /tmp/rpm-meridian/*.rpm /tmp/rpm-minion/*.rpm /tmp/rpm-sentinel/*.rpm; do
  # give it 3 tries then die
  publishPackage cloudsmith push rpm --no-wait-for-sync "${PROJECT}/$REPO/any-distro/any-version" "$FILE" ||
  publishPackage cloudsmith push rpm --no-wait-for-sync "${PROJECT}/$REPO/any-distro/any-version" "$FILE" ||
  publishPackage cloudsmith push rpm --no-wait-for-sync "${PROJECT}/$REPO/any-distro/any-version" "$FILE" || exit 1
done

#for FILE in /tmp/deb-meridian/*.deb /tmp/deb-minion/*.deb /tmp/deb-sentinel/*.deb; do
#  # give it 3 tries then die
#  publishPackage cloudsmith push deb --no-wait-for-sync "${PROJECT}/$REPO/any-distro/any-version" "$FILE" ||
#  publishPackage cloudsmith push deb --no-wait-for-sync "${PROJECT}/$REPO/any-distro/any-version" "$FILE" ||
#  publishPackage cloudsmith push deb --no-wait-for-sync "${PROJECT}/$REPO/any-distro/any-version" "$FILE" || exit 1
#done

for FILE in /tmp/oci-*/*.oci; do
  # give it 3 tries then die
  publishPackage cloudsmith push docker --no-wait-for-sync "${PROJECT}/$REPO" "$FILE" ||
  publishPackage cloudsmith push docker --no-wait-for-sync "${PROJECT}/$REPO" "$FILE" ||
  publishPackage cloudsmith push docker --no-wait-for-sync "${PROJECT}/$REPO" "$FILE" || exit 1
done
