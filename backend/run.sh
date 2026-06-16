#!/usr/bin/env bash
# Run the FORZE backend locally: loads backend/.env into the environment and
# pins Java 25 (project target) so Maven doesn't fork the wrong JDK.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
else
  echo "warning: ${ENV_FILE} not found; relying on existing environment" >&2
fi

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}"

cd "${SCRIPT_DIR}"
exec mvn spring-boot:run "$@"
