# 🎵 AMIS — Adaptive Music Intelligence System

> A full-stack music streaming and playlist management web application with an AI-powered recommendation engine, collaborative editing, and tier-based offline storage.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Demo Accounts](#demo-accounts)
- [API Overview](#api-overview)
- [Database Schema](#database-schema)
- [Business Logic Modules](#business-logic-modules)
- [Subscription Tiers](#subscription-tiers)
- [Known Bug Fixes](#known-bug-fixes)
- [Future Enhancements](#future-enhancements)

---

## Overview

AMIS (Adaptive Music Intelligence System) is a production-grade music platform that addresses common limitations in digital music apps — static playlists, no personalization, absent offline management, and broken collaborative editing. It integrates an intelligent backend with a responsive React frontend to deliver a complete listener experience.

**Key capabilities:**
- Personalized song recommendations scored by play count, genre affinity, and time-of-day context
- Mood-based smart playlist generation
- Collaborative playlist editing with real-time conflict detection and resolution
- Tier-based offline download management with LRU eviction
- Play analytics tracking listening patterns
- JWT-secured role-based access control (ADMIN / USER)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + JavaScript (SPA) |
| Backend | Spring Boot 3 |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA (Hibernate) |
| Security | Spring Security 6 + JWT (HMAC-SHA256) |
| API Style | REST (HTTP conventions) |
| Testing | Postman |

---

## Features

### 🎧 Core Music Features
- Full song catalogue with genre badges and play counts
- Music player with global state (play, pause, queue)
- Like/favourite songs

### 🤖 AI & Intelligence (BL3, BL5)
- **Recommendation Engine** — scores songs using play count (40%), recency (40%), and genre affinity (20%) with a time-of-day bonus; adjustable `explorationLevel` (0–100) controls familiar vs. discovery ratio
- **Smart Playlist Generator** — mood-to-genre mapping (Energetic, Calm, Focus, Party, Sleep) with genre frequency, artist frequency, and novelty scoring

### 🤝 Collaboration (BL6)
- Collaborative playlist editing with a 3-step wizard UI
- Conflict detection and deduplication with human-readable resolution messages (e.g., *"Bohemian Rhapsody was added by two people — kept once"*)
- userId always derived from JWT (no spoofed edits)

### 📥 Offline Manager (BL7)
- Tier-based storage quotas (FREE / PREMIUM / FAMILY)
- Priority-based download queue (in-playlist > recently played > liked > manual)
- LRU eviction when quota is exceeded
- Re-download support for evicted tracks

### 📊 Analytics (BL4)
- Total plays, top 5 songs, top 5 genres
- Chronological listening history

### 💳 Payments & Subscriptions (BL8)
- Subscription upgrade flow with invoice generation
- Tier immediately reflected in storage quota

### 🛡 Admin Panel
- Upload, edit, delete songs in the catalogue
- Platform-wide user management

---

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    React SPA (port 3000)                 │
│         Pages · Components · AuthContext · API Layer     │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP REST + JWT
┌────────────────────────▼────────────────────────────────┐
│              Spring Boot 3 API (port 8080)               │
│    Controllers → Services → Repositories → Entities      │
└────────────────────────┬────────────────────────────────┘
                         │ JPA / Hibernate
┌────────────────────────▼────────────────────────────────┐
│                  PostgreSQL 15 (port 5432)               │
│                       amis_db                            │
└─────────────────────────────────────────────────────────┘
```

---

## Project Structure

### Backend (`com.amis`)
```
config/          → SecurityConfig, JwtUtil
controller/      → 10 REST controllers
dto/             → Request & Response DTOs
filter/          → JwtAuthFilter
model/           → 10 entities + BaseEntity
repository/      → 11 repositories
service/
  ├── interfaces/
  └── impl/      → Service implementations
```

### Frontend (`src/`)
```
context/         → AuthContext, PlayerContext
components/      → Layout, MusicPlayer, PremiumGate, AdBanner
pages/           → 13 route pages
services/
  └── api.js     → Axios instance with JWT interceptors
```

---

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 15

### Backend Setup

```bash
# 1. Create database
psql -U postgres -c "CREATE DATABASE amis_db;"

# 2. Configure connection in application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/amis_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

# 3. Run the Spring Boot app
./mvnw spring-boot:run
# Server starts at http://localhost:8080
```

### Frontend Setup

```bash
cd frontend
npm install
npm start
# App starts at http://localhost:3000
```

> Demo accounts are seeded automatically on first startup via `@PostConstruct`.

---

## Demo Accounts

Three accounts are auto-created on startup — no manual SQL required.

| Account | Email | Password | Role | Tier |
|---|---|---|---|---|
| Admin User | admin@amis.com | admin123 | ADMIN | FREE |
| Alice | alice@amis.com | alice123 | USER | FREE |
| Bob | bob@amis.com | bob123 | USER | PREMIUM |

A **Quick Login panel** on the login page provides one-click access to all three accounts.

---

## API Overview

| Controller | Base Path | Key Endpoints |
|---|---|---|
| Auth | `/auth` | `POST /register`, `POST /login` |
| Songs | `/songs` | Full CRUD |
| Playlists | `/playlists` | CRUD + song management |
| Recommendations | `/recommendations` | `POST /recommendations`, `POST /recommendations/surprise` |
| Smart Playlist | `/smart-playlist` | `POST /smart-playlist/generate` |
| Collaborative | `/collaborative` | CRUD + `POST /{id}/edit` + `POST /{id}/merge` |
| Offline | `/offline` | `POST /queue`, `POST /process`, `GET /tracks`, `GET /storage` |
| Analytics | `/analytics` | `GET /history`, `GET /top-songs`, `GET /top-genres` |
| Payments | `/payments` | `POST /initiate`, `GET /invoices` |
| Users | `/users` | `GET /me`, `GET /all` (ADMIN only) |

All protected endpoints require a `Bearer <token>` in the `Authorization` header.

---

## Database Schema

**Tables:** `users`, `songs`, `playlists`, `playlist_songs`, `user_history`, `play_events`, `offline_tracks`, `collaborative_playlists`, `playlist_edits`, `liked_songs`, `payments`

All entities extend `BaseEntity` which provides `id`, `createdAt`, and `updatedAt` with automatic JPA lifecycle management.

**Key Relationships:**
- User → Playlists (1:many)
- User → UserHistory (1:many)
- User → OfflineTracks (1:many)
- CollaborativePlaylist → PlaylistEdits (1:many)
- User → Payments (1:many)

---

## Business Logic Modules

| Module | Description |
|---|---|
| **BL1** — Playlist Management | Create playlists, add/remove/reorder songs with contiguous position tracking |
| **BL2** — Liked Songs & Discovery | Like songs; liked status boosts offline download priority |
| **BL3** — Recommendation Engine | Weighted scoring: play count + recency + genre affinity + time-of-day bonus |
| **BL4** — Play Analytics | Aggregate play events → top songs, top genres, listening history |
| **BL5** — Smart Playlist Generator | Mood → genre mapping → scored candidate songs sorted by affinity + novelty |
| **BL6** — Collaborative Editing | Multi-user playlist edits with conflict detection and name-based resolution messages |
| **BL7** — Offline Download Manager | Priority queue + LRU eviction + tier quota enforcement |
| **BL8** — Payment & Subscription | Upgrade flow, invoice generation, immediate tier activation |

---

## Subscription Tiers

| Tier | Storage Quota | Price |
|---|---|---|
| FREE | 500 MB (~111 songs) | ₹0 |
| PREMIUM | 2 GB (~455 songs) | ₹199/month |
| FAMILY | 5 GB (~1137 songs) | ₹399/month |

---

## Known Bug Fixes

| Bug | Description | Fix |
|---|---|---|
| BUG1 | Evicted songs could not be re-downloaded | Reset EVICTED records to QUEUED on re-queue |
| BUG2 | Offline quota was hardcoded to FREE tier for all users | Read quota from `QUOTA_MAP` using actual `subscriptionTier` |
| BUG3 | No demo accounts — manual DB seeding required | `@PostConstruct` seeds three demo users on startup |
| BUG4 | `subscriptionTier` missing from login response | Added `subscriptionTier` to auth response DTO |
| BUG5 | No tier selector on registration page | Added visual subscription tier selector on Register page |
| BUG6 | `userId` accepted in collaborative edit request body (spoofable) | `userId` now derived exclusively from JWT principal |
| BUG7 | Conflict messages showed raw song IDs | Conflict messages now use human-readable song names |

---

## Future Enhancements

- **Real-Time Audio Streaming** — Integrate AWS S3 / MinIO to serve actual audio files
- **WebSocket Collaborative Editing** — Live multi-user playlist sync via STOMP over SockJS
- **ML Recommendations** — Replace rule-based scoring with collaborative filtering (matrix factorisation)
- **Social Features** — User following, listen parties, social feed
- **Mobile App** — React Native client consuming the same REST APIs
- **Microservices** — Split into Auth, Catalogue, Recommendation, and Offline services over Kafka
- **Notifications** — SMS/email alerts via Twilio or SendGrid

---

## License

This project was developed as a Mini Project submission. All rights reserved.
