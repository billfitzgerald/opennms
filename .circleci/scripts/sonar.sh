#!/bin/bash

set -e
set -o pipefail

FIND_TESTS_DIR="target/find-tests"

generate_jacoco_report_files()
{
  find . -type f '!' -path './.git/*' -name jacoco.xml
}

generate_junit_report_folders()
{
  find . -type d '!' -path './.git/*' -a \( -name surefire-reports\* -o -name failsafe-reports\* \)
}

generate_class_folders()
{
  generate_junit_report_folders \
    | sed -e 's,/\(surefire-reports\|failsafe-reports\).*$,,' \
    | sort -u \
    | while read -r DIR; do \
      find "$DIR" -maxdepth 1 -type d -name classes; \
    done
}

generate_test_class_folders()
{
  generate_junit_report_folders \
    | sed -e 's,/\(surefire-reports\|failsafe-reports\).*$,,' \
    | sort -u \
    | while read -r DIR; do \
      find "$DIR" -maxdepth 1 -type d -name test-classes; \
    done
}

find_tests()
{
    perl -pi -e 's,/home/circleci,/root,g' target/structure-graph.json

    # Now determine the Maven modules related to the tests we need to run
    cat "${FIND_TESTS_DIR}"/*_classnames | python3 .circleci/scripts/find-tests/find-tests.py generate-test-modules \
      --output=/tmp/this_node_projects \
      .
}

dnf -y remove npm nodejs-full-i18n
dnf module reset -y nodejs
dnf module enable -y nodejs:14
dnf module switch-to -y nodejs:14
dnf -y install npm nodejs-full-i18n

# shellcheck disable=SC1091
. ./.circleci/scripts/lib.sh

PR_NUM="$(get_pr_num || echo 0)"
REFERENCE_BRANCH="$(get_reference_branch || echo "develop")"

echo "#### Making sure git is up-to-date"
if [ -n "${REFERENCE_BRANCH}" ]; then
  git fetch origin "${REFERENCE_BRANCH}"
fi

echo "#### Enumerating Affected Tests and Projects"
find_tests
PROJECT_LIST="$(< /tmp/this_node_projects paste -s -d, -)"
if [ -z "${PROJECT_LIST}" ]; then
  echo "WARNING: no projects found, skipping sonar run"
  exit 0
fi

echo "#### Unpacking Sonar CLI"
unzip -o -q -d /tmp /tmp/sonar-scanner-cli.zip
SONAR_DIR="$(find /tmp -type d -name sonar-scanner\*)"

echo "#### Determining Arguments for Sonar CLI"
declare -a SONAR_ARGS=(
  -Dsonar.projectKey="$SONARCLOUD_PROJECTKEY"
  -Dsonar.organization="$SONARCLOUD_ORG"
  -Dsonar.login="$SONARCLOUD_LOGIN"
  -Dsonar.host.url="https://sonarcloud.io"
  -Dsonar.c.file.suffixes=-
  -Dsonar.cpp.file.suffixes=-
  -Dsonar.objc.file.suffixes=-
)

if [ "${PR_NUM}" -gt 0 ]; then
  SONAR_ARGS=("${SONAR_ARGS[@]}" "-Dsonar.pullrequest.key=${PR_NUM}" "-Dsonar.pullrequest.branch=${CIRCLE_BRANCH}" "-Dsonar.pullrequest.base=${REFERENCE_BRANCH}")
  SONAR_ARGS=("${SONAR_ARGS[@]}" "-Dsonar.pullrequest.provider=GitHub" "-Dsonar.pullrequest.github.repository=OpenNMS/opennms")
else
  SONAR_ARGS=("${SONAR_ARGS[@]}" "-Dsonar.branch.name=${CIRCLE_BRANCH}")
  if [ -n "${REFERENCE_BRANCH}" ] && [ "${REFERENCE_BRANCH}" != "${CIRCLE_BRANCH}" ]; then
    SONAR_ARGS=("${SONAR_ARGS[@]}" "-Dsonar.newCode.referenceBranch=${REFERENCE_BRANCH}")
  fi
fi

export SONAR_SCANNER_OPTS="-Xmx7g"

echo "#### Executing Sonar"
# shellcheck disable=SC2086
"${SONAR_DIR}/bin/sonar-scanner" \
  "${SONAR_ARGS[@]}" \
  -Dsonar.java.source=11 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dsonar.coverage.jacoco.xmlReportPaths="$(generate_jacoco_report_files | paste -s -d, -)" \
  -Dsonar.junit.reportPaths="$(generate_junit_report_folders | paste -s -d, -)" \
  -Dsonar.java.binaries="$(generate_class_folders | paste -s -d, -)" \
  -Dsonar.java.libraries="${HOME}/.m2/repository/**/*.jar,**/*.jar" \
  -Dsonar.java.test.binaries="$(generate_class_folders | paste -s -d, -)" \
  -Dsonar.java.test.libraries="${HOME}/.m2/repository/**/*.jar,**/*.jar"
