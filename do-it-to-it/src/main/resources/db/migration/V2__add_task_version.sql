-- Optimistic locking: bumped on every change, so that an edit based on an out-of-date copy of a task
-- (say, from the console while the web app changed it) is refused instead of silently overwriting.
alter table task add column version bigint default 0 not null;
