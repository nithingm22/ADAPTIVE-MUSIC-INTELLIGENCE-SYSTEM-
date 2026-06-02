# AMIS — Bug Fixes & Improvements

This document lists every change made, what was broken, why it was broken,
and exactly how it was fixed.

---

## BACKEND FIXES

---

### FIX 1 — Evicted songs could never be re-downloaded (BL7)

**File:** `service/impl/OfflineDownloadServiceImpl.java`

**What was broken:**
When a song was auto-evicted to free storage space, its `OfflineTrack` record
remained in the database with `status = "EVICTED"`. The `queueDownload()` method
checked `existsByUserIdAndSongId()` which returned `true` for the evicted record
and immediately returned it — silently blocking the re-download with no error.
The user had no way to download the song again.

**The fix:**
```java
// BEFORE (bug):
if (offlineTrackRepository.existsByUserIdAndSongId(user.getId(), songId)) {
    return offlineTrackRepository.findByUserIdAndSongId(...).get();  // returns EVICTED!
}

// AFTER (fixed):
Optional<OfflineTrack> existing = offlineTrackRepository.findByUserIdAndSongId(...);
if (existing.isPresent()) {
    OfflineTrack track = existing.get();
    if ("EVICTED".equals(track.getStatus())) {
        track.setStatus("QUEUED");               // reset to re-downloadable
        track.setPriority(determinePriority(...)); // re-score priority
        track.setDownloadedAt(null);              // clear old timestamp
        track.setLastPlayedOffline(null);         // reset LRU clock
        return offlineTrackRepository.save(track);
    }
    return track;  // QUEUED or DOWNLOADED — nothing to do
}
```

**Frontend change:** `OfflinePage.js` now shows a "Re-download" button next to
every evicted track. The button calls `POST /offline/queue` with the same songId.

---

### FIX 2 — Premium/Family offline quota was never used (BL7)

**Files:** `model/User.java`, `service/impl/OfflineDownloadServiceImpl.java`

**What was broken:**
The `User` entity had no `subscriptionTier` field. The `OfflineDownloadServiceImpl`
had a quota map with `FREE=500 MB`, `PREMIUM=2048 MB`, `FAMILY=5120 MB` but
always called `QUOTA_MAP.get("FREE")` hardcoded — so every user, regardless of
their plan, was limited to 500 MB. PREMIUM and FAMILY tiers were unreachable dead code.

**The fix in `User.java`:**
```java
// ADDED:
@Column(name = "subscription_tier", nullable = false)
private String subscriptionTier = "FREE";
```

**The fix in `OfflineDownloadServiceImpl.java`:**
```java
// BEFORE:
double quotaMb = QUOTA_MAP.get("FREE");   // always 500 MB

// AFTER:
double quotaMb = getUserQuota(user);       // reads user.subscriptionTier

private double getUserQuota(User user) {
    String tier = user.getSubscriptionTier();
    return QUOTA_MAP.getOrDefault(tier != null ? tier.toUpperCase() : "FREE",
                                 QUOTA_MAP.get("FREE"));
}
```

Also fixed in `getStorageSummary()` so the frontend receives the correct
`subscriptionTier` and `quotaMb` values.

---

### FIX 3 — No pre-created demo accounts (Auth)

**File:** `service/impl/AuthServiceImpl.java`

**What was broken:**
The app started with an empty users table. To get an admin account you had to
manually run a SQL `UPDATE`. There was no way to quickly switch between users
with different roles/tiers to test different features.

**The fix:**
Added `@PostConstruct seedDemoAccounts()` which runs once on startup and creates
three ready-to-use accounts if they don't already exist:

| Email            | Password  | Role  | Tier    | Purpose                          |
|------------------|-----------|-------|---------|----------------------------------|
| admin@amis.com   | admin123  | ADMIN | FREE    | Manage songs, access admin panel |
| user1@amis.com   | user123   | USER  | FREE    | 500 MB offline quota             |
| user2@amis.com   | user123   | USER  | PREMIUM | 2048 MB offline quota            |

---

### FIX 4 — subscriptionTier missing from auth response

**File:** `dto/response/AuthResponse.java`

**What was broken:**
After login, the frontend only received `token`, `name`, `email`, `role`.
The `subscriptionTier` was not returned, so the frontend had to make an extra
`GET /offline/storage` call just to find out the user's plan.

**The fix:**
Added `subscriptionTier` field to `AuthResponse`. Now the sidebar and offline
page know the plan immediately on login without an extra API call.

---

### FIX 5 — subscriptionTier not accepted during registration

**File:** `dto/request/RegisterRequest.java`

**What was broken:**
New users could not choose a subscription tier when signing up —
it always defaulted to FREE with no way to change it.

**The fix:**
Added optional `subscriptionTier` field to `RegisterRequest`.
`AuthServiceImpl.register()` now reads and stores it.

---

### FIX 6 — BL6 edit endpoint required userId in request body

**File:** `controller/CollaborativePlaylistController.java`

**What was broken:**
`POST /collaborative/{id}/edit` required `{ "userId": 2, "songId": 3, ... }`.
This meant:
 1. Users had to know their own numeric ID
 2. Any user could record a fake edit as any other user by passing a different userId

**The fix:**
The controller now derives `userId` from the JWT token (`auth.getName()` →
look up user by email → get ID). The `userId` field is no longer accepted in
the request body.

New request format:
```json
{ "songId": 3, "editType": "ADD", "position": 2 }
```

---

### FIX 7 — BL6 conflict messages used song IDs, not names

**File:** `service/impl/CollaborativePlaylistServiceImpl.java`

**What was broken:**
Conflict notifications said things like:
```
⚠️ Conflict resolved: Song #5 was added by multiple users — kept once.
```

No one knows which song is #5 without looking it up separately.

**The fix:**
Added `getSongTitle(Long songId)` helper. Conflict messages now say:
```
✅ Fixed: "Bohemian Rhapsody" was added by two people — kept once (no duplicates).
```

Also added validation in `recordEdit()` — if the songId doesn't exist,
a clear error message is returned with a hint to use `GET /songs`.

---

## FRONTEND NEW/CHANGED FILES

---

### LoginPage.js — Quick Login Panel

**What was missing:**
No easy way to switch between user accounts for testing.

**What was added:**
Three one-click "Quick Login" cards at the top of the login page showing:
- Account name, email, role badge, tier badge
- Short description of what that account is useful for testing

---

### RegisterPage.js — Subscription Tier Selector

**What was missing:**
Users could not choose a subscription tier at registration.

**What was added:**
Visual tier selector with FREE / PREMIUM / FAMILY options,
each showing the storage amount. Selected tier is sent in the
registration request and stored on the user.

---

### CollaborativePage.js — Simplified BL6 UI

**What was complex:**
- Users had to type song IDs manually (no one knows the numbers)
- userId had to be typed manually in the body
- No guidance on how to create conflict scenarios
- Conflict messages showed raw song IDs

**What was improved:**
- Song picker is a dropdown populated from `GET /songs` (title + artist shown)
- userId is automatic (from who you're logged in as)
- Step-by-step wizard: Setup → Record Edits → Merge Result
- Conflict scenario hints panel explaining what to try
- Persistent Song Reference sidebar with all IDs visible
- Conflict notifications now show song names

---

### OfflinePage.js — Premium UI + Re-download

**What was broken/missing:**
- Storage gauge always showed "FREE" and 500 MB cap
- Evicted tracks had no action button — stuck forever
- Song picker required manual ID entry

**What was fixed:**
- Tier banner shows correct plan (FREE / PREMIUM / FAMILY) with correct quota
- Storage gauge colour changes: green (<60%) → yellow (<85%) → red (>85%)
- Evicted tracks have a "Re-download" button (calls `POST /offline/queue` again)
- Song picker is a dropdown with title, artist, genre shown
- Track count cards show Downloaded, Queued, and Evicted separately

---

### SongsReferencePage.js — NEW PAGE at /songs-reference

**What was missing:**
No centralized place to look up song IDs. Users had to guess or check
the database directly.

**What was added:**
A searchable, filterable song catalog page that:
- Shows every song with its numeric ID
- "Copy ID" button copies to clipboard
- Filter by genre
- Search by title or artist
- Genre colour-coded cards
- SQL seed reminder if no songs loaded

---

### Layout.js — Tier Badge + Song Catalog Link

- User panel at bottom of sidebar now shows tier badge (colour-coded)
- "Song Catalog" link added to nav pointing to `/songs-reference`

---

### AuthContext.js — Stores subscriptionTier

Added `subscriptionTier` to the auth session object so all pages
can access it without extra API calls.

---

### App.js — New Route

Added `/songs-reference` route pointing to `SongsReferencePage`.

---

## HOW TO APPLY THESE CHANGES

1. Copy backend files from `backend/src/main/java/com/amis/` into your project,
   overwriting the originals.

2. Copy frontend files from `frontend/src/` into your project,
   overwriting the originals.

3. Run the backend:
   ```bash
   cd backend
   mvn clean install -DskipTests
   mvn spring-boot:run
   ```
   On first start, the 3 demo accounts are auto-created. Check the console for:
   ```
   [AMIS] Demo account created → admin@amis.com (ADMIN / FREE)
   [AMIS] Demo account created → user1@amis.com (USER / FREE)
   [AMIS] Demo account created → user2@amis.com (USER / PREMIUM)
   ```

4. Run the frontend:
   ```bash
   cd frontend
   npm install
   npm start
   ```

5. Go to http://localhost:3000/login — use the Quick Login buttons.

---

## DEMO ACCOUNTS QUICK REFERENCE

| Email          | Password | Role  | Tier    | Best for                         |
|----------------|----------|-------|---------|----------------------------------|
| admin@amis.com | admin123 | ADMIN | FREE    | Adding songs, admin panel        |
| user1@amis.com | user123  | USER  | FREE    | BL1–BL6, 500 MB offline          |
| user2@amis.com | user123  | USER  | PREMIUM | BL7 premium quota test (2 GB)    |

---

## SONG IDs QUICK REFERENCE (after seeding)

| ID | Title                | Artist         | Genre      |
|----|----------------------|----------------|------------|
| 1  | Bohemian Rhapsody    | Queen          | rock       |
| 2  | Blinding Lights      | The Weeknd     | pop        |
| 3  | So What              | Miles Davis    | jazz       |
| 4  | Moonlight Sonata     | Beethoven      | classical  |
| 5  | Lose Yourself        | Eminem         | hip-hop    |
| 6  | Strobe               | deadmau5       | electronic |
| 7  | Weightless           | Marconi Union  | ambient    |
| 8  | Hotel California     | Eagles         | rock       |
| 9  | Shape of You         | Ed Sheeran     | pop        |
| 10 | Take Five            | Dave Brubeck   | jazz       |
| 11 | Clair de Lune        | Debussy        | classical  |
| 12 | HUMBLE.              | Kendrick Lamar | hip-hop    |
| 13 | Levels               | Avicii         | electronic |
| 14 | River Flows in You   | Yiruma         | ambient    |
| 15 | Thunderstruck        | AC/DC          | rock       |
| 16 | Bad Guy              | Billie Eilish  | pop        |
| 17 | Autumn Leaves        | Chet Baker     | jazz       |
| 18 | Symphony No. 5       | Beethoven      | classical  |

IDs may differ if songs were inserted in a different order.
Use `/songs-reference` page or `GET /songs` to confirm actual IDs.
