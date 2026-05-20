# TransitPulse

TransitPulse to aplikacja do zgłaszania i śledzenia incydentów komunikacji miejskiej.

Projekt pozwala użytkownikom raportować problemy na trasach, potwierdzać zgłoszenia innych osób oraz otrzymywać powiadomienia o ważnych zdarzeniach.

Backend aplikacji został zbudowany z użyciem Spring Boot i udostępnia REST API odpowiedzialne za autoryzację, obsługę zgłoszeń, moderację oraz powiadomienia. 
Frontend korzystający z tego API został napisany w React, TypeScript i Vite.

## Frontend

Repozytorium frontendu:

```text
https://github.com/grzechuzz/transit_pulse_frontend
```

Domyślnie frontend powinien komunikować się z backendem pod adresem:

```text
http://localhost:8080
```

## Wymagania

- Docker i Docker Compose
- Node.js i npm dla frontendu

## Uruchomienie całego projektu

Terminal 1 - backend:

```bash
cd TransitPulse_API
cp .env.example .env
docker compose up -d --build
```

Terminal 2 - frontend:

```bash
cd transit_pulse_frontend
npm install
npm run dev
```

Po uruchomieniu:

- frontend: `http://localhost:5173`
- backend API: `http://localhost:8080`

## Stack

- Java 25
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- Lombok
- JUnit
- Mockito
- Docker
- React 19 
- TypeScript 
- Vite
- npm