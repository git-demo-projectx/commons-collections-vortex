# AGENTS.md

This repository is a snapshot of Apache Commons Collections 4 (Apache License 2.0).
The source level is Java 8. The build uses Maven 3 and JDK 17.

## Commands

| Task | Command |
|---|---|
| Download dependencies and compile | `make setup` |
| Checks: Apache RAT, PMD, Checkstyle | `make lint` |
| All tests | `make test` |
| One test class | `mvn -B -ntp -q test -Dtest=ClassName` |

CI runs `make lint`, the tests with JaCoCo coverage, and a SonarQube Cloud analysis.

## Conventions

- Match the code around you. Do not refactor code outside the task.
- Keep the Apache license header on every new file.
- Add or update tests for every behavior change. Do not weaken an assertion to make a test pass.
