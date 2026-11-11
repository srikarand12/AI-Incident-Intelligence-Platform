CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS runbooks (
  id BIGSERIAL PRIMARY KEY,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  search_vector tsvector GENERATED ALWAYS AS (
    to_tsvector('english', coalesce(title,'') || ' ' || coalesce(content,''))
  ) STORED
);
CREATE INDEX IF NOT EXISTS idx_runbooks_search ON runbooks USING GIN(search_vector);
INSERT INTO runbooks (title, content)
SELECT 'Kafka consumer lag','Check consumer group lag, partition assignment, consumer processing time, dead-letter topics, and recent deployment changes.'
WHERE NOT EXISTS (SELECT 1 FROM runbooks WHERE title='Kafka consumer lag');
INSERT INTO runbooks (title, content)
SELECT 'API latency','Compare p95 and p99 latency, inspect distributed traces, database query plans, connection pools, and dependency timeouts.'
WHERE NOT EXISTS (SELECT 1 FROM runbooks WHERE title='API latency');
INSERT INTO runbooks (title, content)
SELECT 'Database connectivity','Check connection pool utilization, failed authentication, network reachability, active sessions, and recent schema changes.'
WHERE NOT EXISTS (SELECT 1 FROM runbooks WHERE title='Database connectivity');