# Property Manager

A small property-management CRUD app built with Play Framework 3.0 (Scala
2.13, Slick 3.6, in-memory H2). Originally based on the play-slick
[computer-database sample](https://github.com/playframework/play-slick/tree/main/samples/computer-database).

## Requirements

- JDK 17 or 21
- sbt 1.11+

## Run

```bash
sbt run
```

Then open <http://localhost:9000>. Database evolutions are applied on first
request (click "Apply this script now!" if prompted in dev mode); the
in-memory H2 database is seeded with one example property.

## Test

```bash
sbt test
```

Runs unit specs (DAOs, controllers) and an HtmlUnit browser integration
spec.

## Configuration

- `conf/application.conf` — the production secret is read from the
  `APPLICATION_SECRET` environment variable; never commit a secret.
- `conf/evolutions/default/1.sql` — database schema.
