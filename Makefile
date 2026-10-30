# make targets for the factory's workers. Each one runs the project's own Maven
# checks (see the defaultGoal in pom.xml).
MVN := mvn -B -ntp

.PHONY: setup lint test

setup:
	$(MVN) -q -DskipTests test-compile

lint:
	$(MVN) -q -DskipTests compile apache-rat:check pmd:check checkstyle:check

test:
	$(MVN) -q test
