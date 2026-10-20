# EnchantReveal

**See every enchantment before you click — directly in the original enchanting-table tooltip.**

EnchantReveal pairs a small **Paper server plugin** with a **Fabric client mod**.
The server calculates the three vanilla outcomes, and the client replaces the
single hint ending in `...` with the complete list for the hovered offer.
Enchanting keeps its normal probabilities, XP requirements and lapis costs.
Nothing is printed to chat.

[Download version 1.0.0](https://github.com/ZoniBoy00/EnchantReveal/releases/tag/v1.0.0)

> **Install both components.** The Paper JAR belongs on the server; the Fabric
> JAR and a matching Fabric API belong on each player's Minecraft client.

## What it does

- Shows every enchantment and level for each of the three enchanting options.
- Uses the existing enchanting screen and tooltip, without a replacement menu.
- Preserves the normal tooltip's XP and lapis requirement lines.
- Uses the server's native enchanting calculation, including normal book behavior.
- Keeps the usual one-enchantment hint for players who do not install the mod.
- Uses Minecraft's language setting for built-in enchantment names.
- Needs no commands, permissions, configuration, database or external service.

For example, an offer that normally hints at `Silk Touch ...` can instead show
`Silk Touch`, `Efficiency IV` and `Unbreaking III`, **if those are the actual
outcomes for that item and offer**. The mod does not add these enchantments or
let you choose a different roll.

## Compatibility

| Minecraft version | Server | Client | Verification |
| --- | --- | --- | --- |
| 26.1.2 | Paper; checked against build 74 | Fabric + matching Fabric API | Compilation, protocol and target checks |
| 26.2 | Paper; checked against build 134 | Fabric + matching Fabric API | Same checks, plus owner-confirmed in-game operation |
| 26.3 | Paper; checked against build 207 | Fabric + matching Fabric API | Compilation, protocol and target checks |

**Java 25 is required.** The same Paper JAR and the same Fabric JAR cover all
three listed versions. Use the Fabric API release intended for your exact
Minecraft version. The mod requires Fabric Loader 0.18.4 or newer; Fabric API
for 26.3 additionally requires Loader 0.19.3 or newer. The source build uses
Loader 0.19.5.

The server plugin explicitly checks the Minecraft version. Other versions,
including 26.1 and 26.1.1, are not included in this release's support list.
Paper derivatives are not independently verified. Forge, NeoForge, Folia,
Bedrock/Geyser clients, snapshots and standalone singleplayer are unsupported.

## Download

Get these two files from [Releases](https://github.com/ZoniBoy00/EnchantReveal/releases):

| File | Install location |
| --- | --- |
| `EnchantReveal-Paper-1.0.0.jar` | Paper server's `plugins/` folder |
| `EnchantReveal-Fabric-1.0.0.jar` | Minecraft client's `mods/` folder |

GitHub's automatically generated **Source code** archives are for building the
project; they are not the ready-to-install JARs. Do not put the Paper JAR in
`mods/` or the Fabric JAR in `plugins/`.

## Server installation

1. Stop the Paper server.
2. Remove any older EnchantReveal JAR from `plugins/`. Keep only one version.
3. Copy `EnchantReveal-Paper-1.0.0.jar` into `plugins/`.
4. Start the server and check that EnchantReveal enables without an exception.

No Fabric installation or Fabric API is needed on the server. There are no
additional ports to open: plugin messages use the existing Minecraft connection.
If you use Pterodactyl, upload the Paper JAR through the panel's file manager
and restart the Minecraft server from the panel.

Use a normal stop/start when installing or replacing the plugin. Dynamic
plugin-manager loading and `/reload` have not been tested for this project.

## Client installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your exact
   Minecraft version, or select the corresponding Fabric instance in your launcher.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for that same version.
3. Put `EnchantReveal-Fabric-1.0.0.jar` in that instance's `mods/` folder.
4. Restart Minecraft and launch the Fabric instance.
5. Join the Paper server with the companion plugin installed.

For a default Windows installation, the folder is usually
`%APPDATA%\.minecraft\mods`. Launchers that manage separate instances use the
instance's own game directory instead. Choose **Open folder** or its equivalent
in the launcher when unsure.

Every player who wants full previews must install the client mod. Other players
can continue using the normal enchanting interface without it.

## Using the preview

1. Open a normal enchanting table.
2. Put an enchantable item and lapis in the table.
3. Hover one of the three offers.
4. After the server response arrives, its tooltip lists the complete outcome.
5. Click the offer normally to enchant the item.

The original hint can remain visible briefly while the request is in flight.
The mod leaves vanilla's hint in place until it has a matching response; it does
not guess missing results. If the item, seed, menu state or offers change,
the old preview is no longer used.

There is no keybind, chat command, configuration screen or permission node to set.

## How it works

The client checks the open enchanting screen's state during its normal tick.
When the source item or offers change, it requests a preview through the
`enchantreveal:request` plugin channel. The Paper plugin checks the active menu
and calls its native `EnchantmentMenu.getEnchantmentList` calculation for all
three options. It returns enchantment identifiers, levels and required costs
through `enchantreveal:preview`.

Requests include a menu ID, state ID and unique request number. The client
matches the reply to the current request and checks its item snapshot, seed,
costs and hints before displaying it. A response for an older state is discarded.
The client resolves enchantment names through Minecraft's registry and replaces
only the first, abbreviated hint portion of the tooltip.

The server does not enchant an item to obtain the preview. Previewing neither
changes the player's enchantment seed nor spends XP or lapis.

### Performance and data handling

- No repeating server task or scan of online players.
- Requests from one player in the same tick are merged into one preview job.
  That job calculates all three offers.
- The client reuses a valid preview rather than requesting it every frame.
- An unanswered request can retry twice, with 20 client ticks between attempts.
- Caches are in memory; there is no player-history file, database or telemetry.
- No HTTP requests, external account, analytics or extra network connection.
- Protocol parsing limits packet sizes and enchantment counts. Malformed
  requests are ignored; malformed or oversized preview replies are not displayed.

This is intended to keep overhead low. CPU, memory, FPS and TPS overhead have
not been measured in a controlled benchmark or large-server load test.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| The tooltip still shows only one enchantment and `...` | Confirm both 1.0.0 JARs are installed on the correct sides, restart Minecraft and the server, and verify that the launched client is the Fabric instance. |
| Minecraft refuses to load the mod | Use Java 25, a listed Minecraft version, Fabric Loader and a matching Fabric API. Remove duplicate mod JARs. |
| Paper disables EnchantReveal at startup | Check the server version and the exception immediately after the EnchantReveal log entry. This release accepts only 26.1.2, 26.2 and 26.3. |
| A preview appears after a short delay | The result comes from the server. Give the connection a moment to return a matching reply. |
| A preview never updates after a synchronization issue | Close and reopen the table, then reinsert the item. This creates a fresh screen/request state. |
| Enchantments appear in chat | An older chat-output EnchantReveal build is still installed. Remove old copies and restart the server. Version 1.0.0 in this repository has no chat-output feature. |
| The final item differs from the preview | Check for another plugin changing `EnchantItemEvent`, custom enchanting mechanics or an incompatible version. Test without those changes. |
| A custom enchantment does not expand | If its identifier is unknown to the client registry, the mod preserves the normal tooltip. Custom-enchantment compatibility is not guaranteed. |
| Other players only see the usual hint | Those players need the Fabric mod and Fabric API too. The server plugin alone cannot expand a vanilla client's tooltip. |

For a useful bug report, include the exact Minecraft, Paper build, Fabric Loader,
Fabric API and EnchantReveal versions, relevant server/client exception text,
and short reproduction steps. A screenshot of the tooltip can help.

## Limitations

- The server companion is required; this is not a client-only RNG cracker.
- A Fabric singleplayer world does not run the Paper companion plugin.
- A plugin that changes enchantments when `EnchantItemEvent` fires can change
  the final result after this preview. Such plugins need separate testing.
- Mods that replace the enchanting screen or its rendering can conflict with
  the tooltip injection.
- Native server methods and client injection points are version-sensitive.
  Support for a future Minecraft version requires checking and updating the code.

## Source layout

| Directory | Contents |
| --- | --- |
| `source/server/` | Paper plugin and Maven build |
| `source/client/` | Fabric client, tooltip mixin and Gradle build |
| `source/shared/` | Shared wire protocol and standalone protocol tests |

Ready-to-install binaries are distributed through GitHub Releases.

## Building from source

Keep `server`, `client` and `shared` together inside `source/`; both builds
reference the sibling shared source directory.

### Paper plugin

Requirements: JDK 25 and Maven 3.9 or newer.

```sh
cd source/server
mvn clean package
```

Output: `source/server/target/EnchantReveal-1.0.0.jar`. The release asset is the
same plugin packaged under the clearer name `EnchantReveal-Paper-1.0.0.jar`.
Paper API is a provided dependency and is not bundled into the JAR.

### Fabric client

Requirements: JDK 25 and Gradle 9.7.1. This repository does not include a Gradle
wrapper; `gradle` must be available on your PATH.

```sh
cd source/client
gradle build
```

Output: `source/client/build/libs/EnchantReveal-Fabric-1.0.0.jar`.
The default development target is Minecraft 26.1.2 with Fabric API
`0.155.3+26.1.2`. If changing the development target, update both properties in
`source/client/gradle.properties` together. The distributed universal client was
compiled against all three supported versions with identical resulting classes.

### Protocol tests

From the repository root, with JDK 25 on your PATH:

```sh
javac --release 25 -d build/protocol-tests source/shared/src/main/java/fi/zoniboy/enchantreveal/wire/Protocol.java source/shared/src/test/java/ProtocolTest.java
java -cp build/protocol-tests ProtocolTest
```

The standalone test checks multi-enchantment round trips, truncation, unsupported
protocol versions, invalid counts/keys/levels and oversized packets.

## Verification and versioning

Before release, both components were compiled against 26.1.2, 26.2 and 26.3.
Native server signatures, the client tooltip/tick targets and Paper/Fabric
payload byte compatibility were checked for those versions. The server owner
also confirmed the tooltip works in-game on Paper 26.2 build 134 with the Fabric
client. No in-game confirmation is claimed for 26.1.2 or 26.3.

Version **1.0.0** is the initial repository release. It renumbers the previously
tested development build: program class files and wire protocol remain unchanged.
The protocol version is tracked separately from the plugin/mod release version.
