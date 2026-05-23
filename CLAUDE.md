# CLAUDE.md - Project Memory

## Tech Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.3
- **Build Tool:** Maven
- **Database:** SqlLite with JPA/Hibernate

## Coding Standards & Patterns
- **Language Features:** Use modern Java (Records for DTOs, Pattern Matching, Text Blocks).
- **Architecture:** Follow Clean Architecture; strictly separate Controllers, Services, and Repositories.
- **Naming:** CamelCase for classes/methods; suffix DTOs with `Dto` and Entities with nothing.
- **Error Handling:** Use a global `@ControllerAdvice` and custom Exception hierarchy.

## Project Structure
- `src/main/java`: Application source code
- `src/main/resources`: Config files (application.properties/yaml)
- `src/test/java`: Unit and Integration tests
- `docs/`: Detailed architectural ADRs (referenced if needed)

## Stage Instructions
Each time you are asked to implement a stage, base the implementation on the tasks.md and theology-study-tracker-spec.md.
## Running the App

```bash
JAVA_HOME=/Users/jonathan/.sdkman/candidates/java/21.0.11-amzn \
THEOLOGY_DB_PATH=/Users/jonathan/projects/theology-tracker-java/data/theology.db \
/Users/jonathan/.sdkman/candidates/maven/3.9.9/bin/mvn spring-boot:run
```
