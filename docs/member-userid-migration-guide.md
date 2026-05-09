# Member `user_id` Migration Phase 1

## Scope

Phase 1 adds `user_id`-based columns to the main member relationship tables while keeping the existing `username` columns alive.

Target tables:

- `fp_103`
- `fp_105`
- `fp_150`
- `fp_160`
- `fp_40`
- `fp_200`

## Current application behavior

- Existing read logic still works with `username`.
- New writes now populate both `username` and `user_id` columns where the new columns exist.
- This is a dual-write compatibility phase, not the final cutover.

## Apply order

1. Run [member-userid-phase1.sql](/c:/workspace/plate-main/docs/member-userid-phase1.sql) on the database.
2. Deploy the application build containing the dual-write changes.
3. Verify new rows in the target tables have both `username` and `user_id` values populated.

## Next phase

- Add repository/service read paths based on `user_id`.
- Move block/report/friend/search filters to `user_id` joins.
- Add foreign keys to `fp_100.user_id`.
- Remove old `username`-only assumptions after cutover.
