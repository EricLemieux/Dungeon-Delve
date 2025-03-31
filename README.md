# Dungeon Delve

Text-based role playing adventure game, similar to dungeons-and-dragons mixed with zork.

## Running

Run locally in the simplest way. Make sure that you have jdk 17+ installed.

```shell
./gradlew run
```

## TODO

- thinking indicator during the enemy turn in combat
- add logging
- the state doesn't seem to be saved when transitioning to combat, so a second browser isn't seeing the combat when loading after