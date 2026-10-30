# WorldBattles Loading Screens

Mod Forge indépendant pour Minecraft 1.20.1 / Forge 47.2.0 / Java 17.

Le workflow GitHub Actions compile le projet à chaque push sur main et publie le JAR comme artefact de build.

## Build local
```bash
gradle --no-daemon build
```
Le JAR est généré dans `build/libs/`.

Les images personnalisées sont distribuées par le serveur aux clients qui possèdent le mod.
