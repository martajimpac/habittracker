# Onboarding + Auth UI Design

**Date:** 2026-07-17  
**Source:** Figma Make [Habit Tracker App Design](https://www.figma.com/make/HBHNolFUUZYOYFKfI150Kt/Habit-Tracker-App-Design)

## Decisions

- Onboarding shown once; flag in DataStore (`onboarding_completed`).
- After Skip / Get Started → Login.
- Subsequent starts: Onboarding if not done → else Login if no session → else Home.
- ViewModel reads/writes DataStore directly for onboarding (exception to VM→UseCase).
- Session via extended `AuthRepository.isLoggedIn()` (Supabase).
- Copy in English in `strings.xml`.
- Nunito as global app font.
- Swipe + Continue/Skip on onboarding.
- Login/Register match Figma; no Google/Apple buttons.
- Forgot password / Terms: UI only.
- No prefilled credentials in LoginUiState.

## Screens

### Onboarding (3 slides)

1. Build Habits That Actually Stick — purple gradient  
2. Visualize Your Progress — teal gradient  
3. Achieve Your Best Self — amber gradient  

### Login

Logo tile, Welcome back, email/password fields, forgot password, Sign In CTA, Sign up footer.

### Register

Back, Create account, full name / email / password, terms checkbox UI, Create Account CTA.
