# Dungeon Delve

Text-based role playing adventure game, similar to dungeons-and-dragons mixed with zork.

## Running Locally

Run locally in the simplest way. Make sure that you have jdk 17+ installed.

```shell
./gradlew run
```

## Running with Docker Compose

The application can be run using Docker Compose, which simplifies deployment and environment setup.

### Prerequisites

- Docker and Docker Compose installed on your system
- API keys for external services (OpenAI and ElevenLabs)

### Setup

1. Copy the example environment file and add your API keys:

```shell
cp .env.example .env
```

2. Edit the `.env` file and replace the placeholder values with your actual API keys.

### Running the Application

Build and start the application:

```shell
docker-compose up -d
```

The application will be available at http://localhost:8888

To stop the application:

```shell
docker-compose down
```

## Environment Variables

The following environment variables are required for the application to function properly:

| Variable Name       | Description                                                                |
|---------------------|----------------------------------------------------------------------------|
| OPENAI_API_KEY      | API key for OpenAI services. Used for generating text content in the game. |
| ELEVEN_LABS_API_KEY | API key for ElevenLabs services. Used for text-to-speech functionality.    |

## TODO

- the state doesn't seem to be saved when transitioning to combat, so a second browser isn't seeing the combat when loading after
- refactor into different modules
- start creating an adventure
- tests
- pipeline
