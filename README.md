# Via Romana Extras

Two additions to [Via Romana](https://modrinth.com/mod/via-romana): **roads speed up mounts too**,
and **you declare what a road is made of**.

Minecraft 1.20.1 / Forge. Requires Via Romana. **GPL-3.0.**

> Via Romana itself is GPL-3.0. This mod injects into its internals and mirrors the shape of its
> `SpeedHandler`, so it is licensed the same way rather than under the permissive licence used by
> the rest of [eruto-mc](https://github.com/eruto-mc). No Via Romana code is bundled or
> redistributed — see *This is not a fork* below.

## 1. Mounted travel gets the road bonus

Via Romana's `SpeedHandler.onPlayerTick` attaches its `MOVEMENT_SPEED` modifier to the
`ServerPlayer` only. While riding, movement speed comes from the **mount's** attributes, so the
+40% does nothing on horseback. (Scanning every class in the jar, nothing references
`AbstractHorse` at all.)

This attaches the same modifier to the ridden entity, under the same condition as upstream:
`PathGraph.getNearestNode(pos, node_distance_minimum)` must exist — i.e. within 4 blocks of a
recorded route node.

**⚠ If you raise your horses' base speed, check that `base × 1.4 × 43.17` stays under whatever
your fastest other transport is.** The bonus is multiplicative and it is easy to accidentally make
roads faster than rails.

## 2. Declare the road's materials

Via Romana's `isBlockValidPath` is one line: not air, and in the `via_romana:path_block` tag. That
tag is built from ~24 substring matches in the config. On a heavily modded setup that resolves to
a very large set — **1,979 blocks out of 7,783 in ours** — which means desert sandstone, deepslate
bands and basalt deltas all count as road, and route quality can be farmed off natural terrain.

This adds a button to the charting screen opening a 3×3 grid where you declare up to **9 block
types**. Declared blocks replace the tag for path detection. The config needs no editing at all.

- The slots **copy a sample, they do not hold items.** `minecraft:dirt_path` is not obtainable as
  an item in survival (its loot table drops `minecraft:dirt`, and there is no recipe — checked
  against the vanilla jar), and it is the single most central road block in the game. A
  real-item design could not register it. There is a "use the block under my feet" button instead,
  and nothing can be lost by closing the screen.
- The declaration lasts until you quit — **you declare per survey**, by design.
- Undeclared blocks show up **highlighted in-world while surveying**, because Via Romana's
  `InvalidBlockRenderer` calls the same method.

## Client-side only

Both the quality calculation (`ChartingHandler`) and the validity check run on the client, and the
server does not re-verify the quality that `ChartedPathC2S` reports (confirmed by reading the jar).
So no packets or syncing are needed.

**⚠ The flip side: a client without this mod can ignore the declaration.** It is a design tool,
not an anti-cheat measure.

## This is not a fork

It calls Via Romana's public classes. No Via Romana code is bundled or redistributed. Mixins use
`@Inject` only — never `@Redirect` or `@Overwrite` — so other mods can coexist on the same targets.

Both features belong upstream and have been proposed there. **If they land, this mod is retired.**

## Build

```bash
export JAVA_HOME=/path/to/jdk-17      # JDK 17 ならどれでもよい
./gradlew build --no-daemon
# → build/libs/via_romana_extras-<version>.jar
```

## Contributing

Issues and pull requests are welcome. Localisation files live in
`src/main/resources/assets/via_romana_extras/lang/` — currently `en_us` and `ja_jp`.
