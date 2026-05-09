# Security Specification - GameRec Pro

## Data Invariants
1. A user document must have a `uid` that matches the document ID.
2. Only the owner of a user document can read or write to it.
3. Users cannot elevate their own `isPremium` status (though for simplicity here, we might allow it if it's client-side, but ideally it should be server-side).

## The Dirty Dozen Payloads
- Payload 1: Create user with different UID.
- Payload 2: Read another user's profile.
- Payload 3: Update another user's profile.
- Payload 4: Delete another user's profile.
- Payload 5: Inject 1MB string into `displayName`.
- Payload 6: Modify `createdAt` after creation.
- Payload 7: Set `isPremium` to `true` without authorization.
- Payload 8: Create user without required fields.
- Payload 9: List all users.
- Payload 10: Use non-server timestamp for `updatedAt`.
- Payload 11: Spoof email verification.
- Payload 12: Use invalid document ID for user profile.
