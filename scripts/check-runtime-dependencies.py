#!/usr/bin/env python3
"""Check resolved public Maven coordinates against OSV. No source or secrets are sent."""
import json
import sys
import urllib.request
from pathlib import Path


def query_batch(queries):
    request = urllib.request.Request(
        "https://api.osv.dev/v1/querybatch",
        data=json.dumps({"queries": queries}).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=40) as response:
        results = json.load(response)["results"]
    if len(results) != len(queries):
        raise RuntimeError("Incomplete OSV response")
    return results


def main():
    source = Path("target/runtime-dependencies.txt")
    components = {}
    for line in source.read_text().splitlines():
        coordinate = line.strip().split(" -- ", 1)[0].split(":")
        if len(coordinate) in (5, 6) and coordinate[-1] in ("compile", "runtime"):
            name = ":".join(coordinate[:2])
            version = coordinate[-2]
            components[(name, version)] = {
                "package": {"ecosystem": "Maven", "name": name}, "version": version
            }
    if not components:
        raise RuntimeError("No runtime dependencies resolved; run Maven dependency:list first")
    findings = []
    values = list(components.values())
    for offset in range(0, len(values), 100):
        pending = values[offset:offset + 100]
        while pending:
            next_queries = []
            for component, result in zip(pending, query_batch(pending)):
                if result.get("vulns"):
                    findings.append({"component": component, "vulns": result["vulns"]})
                if result.get("next_page_token"):
                    next_queries.append(component | {"page_token": result["next_page_token"]})
            pending = next_queries
    report = {"components": len(values), "findings": findings}
    Path("target/dependency-security.json").write_text(json.dumps(report, indent=2))
    print(json.dumps(report))
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
