### Testing & Quality Assurance
- Unit and integration tests are written in `src/test/java/...`.
- Run tests with `mvn test`.
- Test results are available in `target/surefire-reports/`.
- Code coverage is measured using JaCoCo (configured in `pom.xml`).
- Generate coverage report with `mvn jacoco:report`.
- Coverage report location: `target/site/jacoco/index.html` (HTML format).