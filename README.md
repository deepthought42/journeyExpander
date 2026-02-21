# journeyExpander

`journeyExpander` receives verified journeys and expands them into candidate journeys by appending valid next interactions discovered on the journey’s resulting page.

## Behavior summary
- Accepts a Pub/Sub push-style HTTP POST on `/`.
- Decodes `message.data` (Base64 JSON) into a `VerifiedJourneyMessage`.
- Validates that the incoming journey can be expanded.
- Collects interactive page elements and creates candidate click steps.
- Persists generated steps/journeys and publishes `JourneyCandidateMessage` payloads.

## Request contract
The endpoint expects this shape:

```json
{
  "message": {
    "data": "<base64-encoded VerifiedJourneyMessage JSON>"
  }
}
```

### Response behavior
- `200 OK`: request handled (including “no expansion” scenarios).
- `400 BAD REQUEST`: payload is malformed or journey data is missing required fields.
- `500 INTERNAL SERVER ERROR`: unexpected processing failure.

## Build and run
### Prerequisites
- Java 17
- Maven 3.8+

### Commands
```bash
mvn clean package
mvn spring-boot:run
```

Default application port: `8080`.

## Configuration
Main config files:
- `src/main/resources/application.properties`
- `src/main/resources/application.yml`

Configure GCP project and Pub/Sub topic values using properties or environment overrides for your deployment environment.

## Development
- Commit style follows Conventional Commits (`CONTRIBUTING.md`).
- `scripts/download-core.sh` runs during Maven `validate` to fetch the `com.looksee:core` artifact used by this service.
