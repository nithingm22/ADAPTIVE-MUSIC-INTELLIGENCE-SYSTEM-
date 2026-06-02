@echo off
REM Wait a moment for backend to fully start
timeout /t 3 /nobreak

REM Run the seed SQL script
psql -U postgres -d amis_db -f seed.sql

echo.
echo Seeding complete! You can now:
echo 1. Go to http://localhost:3000
echo 2. Register for an account
echo 3. See the seeded songs in the Dashboard
pause
