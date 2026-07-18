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

### Done

- **Toolchain**: sbt 0.13.16 → 1.11.7, Scala 2.12.4 → 2.13.18
  (2.11 cross-build dropped), builds and tests on Java 21.
- **Framework**: Play 2.6.13 (`com.typesafe.play`) → Play 3.0.11
  (`org.playframework`, Pekko-based). Akka is gone from the dependency
  tree, which also settles the Akka BSL licensing question.
- **Libraries**: play-slick 3.0.3 → 6.2.0, Slick 3.2.1 → 3.6.1,
  H2 1.4.196 → 2.3.232, play-specs2 via Play 3.0.11; explicit
  fluentlenium 3.2.0 and redundant play-jdbc-* pins removed.
- **Deprecated APIs**: `bindFromRequest()` parens, parameterless reverse
  routes, `Symbol("...")` instead of symbol literals in views, Guice
  `configure()` override, logback `converterClass` → `class`,
  FluentLenium package `org.*` → `io.*`, dead `application.name` key
  removed.
- **Bug fixes** (each verified by tests):
  1. Evolutions schema types: `PROPERTY.SURFACE` BIGINT → DOUBLE (values
     were silently truncated), `PRICEHISTORY.PRICE` VARCHAR → DOUBLE.
  2. `update()`/`delete()` discarded the first DAO Future (write race,
     swallowed errors) — now properly sequenced; regression test added.
  3. `ApplicationSpec` revived (was 100% commented out over "random CSRF
     issues") using `CSRFTokenHelper.withCSRFToken` — 5 controller
     examples restored plus 1 new regression example.
  4. Dev-mode evolutions loop (the "SLICK in DEV mode" bug from the old
     README): fixed via `play.evolutions.db.default.autoApply=true`
     (safe: in-memory disposable DB only). Verified by booting `sbt run`
     and fetching `/` and `/prices`.
  5. `/prices` rendered the raw i18n key `prices.list.title`; message
     added.
- **Security**: committed (commented-out) `play.http.secret.key` removed
  from `application.conf`; production secret now comes from the
  `APPLICATION_SECRET` env var. H2 CVE-2021-42392 / CVE-2022-23221 /
  CVE-2021-23463 resolved by the 2.3.232 upgrade; all EOL Play 2.6 /
  Akka 2.5 era transitive dependencies replaced by the Play 3 line.
- **Hygiene**: `.gitignore` covers sbt 1.x/BSP/IDE artifacts; README
  rewritten with accurate run/test instructions.
- **CI**: workflow now tests on Temurin JDK 21 (checkout@v4,
  setup-java@v4, sbt/setup-sbt@v1 with sbt caching were already current);
  the `sbt test` command mirrors what was verified locally.

Test status: baseline **could not run at all** (sbt 0.13.16 unresolvable,
Java-21-only environment). Final: **10 examples, 0 failures** (ModelSpec 3,
ApplicationSpec 6, IntegrationSpec 1 — the HtmlUnit full-browser flow),
plus a manual dev-mode boot check.

### Deferred (deliberate)

- **Secret rotation**: the previously committed secret should be treated
  as leaked and rotated wherever it may have been deployed — operator
  action, not done here.
- **Scala 3 / Play 3.1**: Play 3.1 was not yet the settled stable line at
  the audit cutoff; Scala 3 offers little for ~620 LOC and would churn
  every file. Revisit when Play 3.1.x is established.
- **UI/assets**: 2012-era Bootstrap CSS left untouched (behavior-neutral
  modernization only).
- **Persistent database**: in-memory H2 is intentional for this demo;
  no move to file-based/persistent storage.
- **Ordering/`orderBy` parameter**: the `list` endpoints accept an
  `orderBy` argument that the DAOs ignore (pre-existing). Left as-is —
  implementing it would be a behavior change, not a modernization.

## PR-style summary

**Modernize property-manager: Play 2.6/Scala 2.12/sbt 0.13 → Play 3.0.11/
Scala 2.13.18/sbt 1.11.7 on Java 21**

This branch takes the app from a 2017 stack that no longer builds at all
(sbt 0.13 artifacts unresolvable, JDK-8-only) to the current community
Play line, with zero route/behavior changes apart from documented bug
fixes.

Changes: toolchain and framework migration (Pekko-based Play 3, play-slick
6, Slick 3.6, H2 2.3); deprecated-API cleanup (forms, reverse routes,
symbol literals, Guice, logback); four real bug fixes with regression
coverage (schema type truncation, unsequenced DAO futures, dev-mode
evolutions loop, missing i18n key); revival of the fully-commented-out
controller spec via CSRF test helpers; secret moved to an env var; CI on
JDK 21.

Risks: H2 2.x is stricter SQL-wise than 1.4 (mitigated by fixing the two
mistyped columns; full suite green); FluentLenium/HtmlUnit major-version
jump in the browser test (suite green); `autoApply` evolutions now also
apply in prod mode — acceptable because the app only targets a disposable
in-memory DB.

Untouched: routes, public URLs, templates' rendered output, `public/`
assets, LICENSE, seed data.

