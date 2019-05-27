#!/usr/bin/env bash

# Exit script if a statement returns a non-true return value.
set -o errexit

# Use the error status of the first failure, rather than that of the last item in a pipeline.
set -o pipefail

# shellcheck source=opennms-container/registry-config.sh
source ../registry-config.sh

# shellcheck source=version-n-tags.sh
source ./version-tags.sh

for TAG in ${OCI_TAGS[*]}; do
  docker tag minion "${CONTAINER_REGISTRY}/${CONTAINER_REGISTRY_REPO}/minion:${TAG}"
done
docker images
