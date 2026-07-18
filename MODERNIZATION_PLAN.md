# Modernization Plan — property-manager

Audit date: 2026-07-18. Conservative modernization: preserve behavior and
public routes/APIs unless clearly broken.

## Current state (audited, not assumed)

Small Play Framework CRUD app (~620 LOC Scala + Twirl views) managing
properties and their price history, backed by in-memory H2 via Slick.

| Component | Current | Status |
|---|---|---|
| sbt | 0.13.16 (2017) | EOL; unresolvable here (0.13 artifacts not on Maven Central; GitHub/typesafe repos blocked by proxy) — **build won't run at all** |
| Scala | 2.12.4 (2017) | EOL line for new work; 2.12.4 predates JDK 21 support |
| Play | 2.6.13 (com.typesafe.play, 2018) | EOL since ~2019; multiple CVEs fixed in later lines (e.g. CVE-2020-26882/26883 host/redirect issues, CVE-2021-23339 in later 2.x); Play 2.x commercial-support-only, community line is Play 3.x |
| play-slick | 3.0.3 | EOL, tied to Play 2.6 |
| Slick | 3.2.1 | Old; current 3.6.1 |
| H2 | 1.4.196 | **CVE-2021-42392, CVE-2022-23221 (RCE via JDBC URL/init), CVE-2021-23463 (XXE)** — fixed in 2.x |
| specs2 / play-specs2 | 2.6.13 | via Play; keep specs2 style, upgrade with Play |
| fluentlenium-core | 3.2.0 (explicit) | Redundant — play-test bundles a compatible FluentLenium; explicit old pin conflicts |
| JDK (CI) | 8 (temurin) | Environment has only Java 21; Play 2.6 cannot run on 21 |
| Akka | 2.5.x (transitive via Play 2.6) | Old line; Akka >= 2.7 is BSL-licensed — migrate to Apache Pekko via Play 3 |
| CI | checkout@v4, setup-java@v4, sbt/setup-sbt@v1 | Action majors fine; JDK 8 + sbt 0.13 job cannot work in this environment profile |

### Baseline build/test result

`sbt about` fails before loading the project: the sbt launcher cannot
resolve sbt 0.13.16 (not on Maven Central; typesafe/scala-sbt repos return
403 through the proxy). Tests therefore **won't run** at baseline.
Recorded verbatim: `could not retrieve sbt 0.13.16`.

### Test inventory

- `test/ModelSpec.scala` — DAO CRUD round-trip (real coverage).
- `test/IntegrationSpec.scala` — HtmlUnit browser flow over the UI.
- `test/ApplicationSpec.scala` — fully commented out ("todo fix random
  CSRF issue").

### Bugs found in audit (fix with regression coverage)

1. **Schema type bugs** (`conf/evolutions/default/1.sql`):
   - `PROPERTY.SURFACE` is `BIGINT` but the model maps `Option[Double]` —
     surface values are silently truncated (ModelSpec even asserts the
     truncated value `Some(3.0)` after writing `3.4`).
   - `PRICEHISTORY.PRICE` is `VARCHAR` but mapped as `Double` — relies on
     lenient H2 1.x implicit casts; breaks under H2 2.x strictness.
2. **Unsequenced futures** (`app/controllers/Application.scala`): in
   `update`, `save`, `delete` the first DAO call's `Future` is discarded
   (`{ a(); b() }` inside a for-comprehension sequences only `b`), so the
   redirect can happen before the write completes, and errors in the first
   call are silently dropped.
3. **Committed application secret** (`conf/application.conf`): a real-looking
   `play.http.secret.key` value is present (commented out). Security issue:
   treat as leaked; do not re-commit; use an env-var override instead.
   Rotation of any deployed secret is the operator's job.

## Prioritized checklist

### P0 — toolchain + framework (coupled; app cannot build otherwise)
- [ ] sbt 0.13.16 → 1.11.7 (`project/build.properties`)
- [ ] Scala 2.12.4 → 2.13.16 (drop 2.11 cross-build)
- [ ] Play 2.6.13 (com.typesafe.play) → Play 3.0.11 (org.playframework,
      Pekko-based — resolves Akka licensing question entirely)
- [ ] play-slick 3.0.3 → 6.2.0; Slick 3.2.1 → 3.6.1
- [ ] H2 1.4.196 → 2.3.232 (CVE fixes above)
- [ ] Drop explicit fluentlenium 3.2.0 (use play-test's bundled one)
- [ ] Code migration: `bindFromRequest` → `bindFromRequest()`, config,
      routes untouched, views untouched unless compiler says otherwise
- [ ] Green `sbt test` gate before anything else lands on top

### P1 — bug fixes (each with regression test)
- [ ] Fix evolutions schema types (SURFACE → DOUBLE, PRICE → DOUBLE);
      update ModelSpec to assert un-truncated surface
- [ ] Sequence DAO futures properly in `update`/`save`/`delete`
- [ ] Re-enable `ApplicationSpec` if feasible (CSRF: form posts in tests
      need CSRF token or test-mode config) — otherwise document why not

### P2 — hygiene / security
- [ ] `application.conf`: remove commented-out secret, add
      `play.http.secret.key = ${?APPLICATION_SECRET}` env override; note
      leak in this plan
- [ ] `.gitignore`: add sbt 1.x dirs (`project/target`, `.bsp`, metals/bloop)
- [ ] README: modern run instructions (the CTRL+D dance is a Play-2.6-era
      workaround; document current behavior)
- [ ] `application.name` config key is dead since Play 2.6 — drop or move

### P3 — CI
- [ ] `.github/workflows/ci.yml`: temurin 21, keep checkout@v4 /
      setup-java@v4 / setup-sbt@v1, sbt caching; command mirrors the
      locally verified `sbt test`

## Deliberately out of scope (conservative)
- No Scala 3 migration (Play sbt plugin + Twirl fine on 2.13; low value here).
- No Play 3.1 (still had milestone-only artifacts for some modules at
  cutoff-adjacent checks; 3.0.x is the stable line).
- No UI/Bootstrap rewrite; `public/` assets untouched.
- No DB change (in-memory H2 is the point of this demo app).

## Done vs Deferred

Filled in at the end of the work — see bottom of file.
