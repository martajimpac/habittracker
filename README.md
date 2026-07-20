# Habit Tracker 

A modern Android habit tracking application built with **Jetpack Compose**, following **Clean Architecture** and **MVVM** principles.

The goal of this project is to explore modern Android development practices while building a production-ready application focused on helping users create and maintain positive habits.

> **Status:** 🚧 Under active development

---

## ✨ Features

### Authentication
- Email & password authentication
- Secure session management
- Persistent login
- Onboarding flow for first-time users

### Habit Management
- Create, edit and delete habits
- Habit categories
- Custom colors and icons
- Daily progress tracking
- Current and longest streaks

### Statistics
- Habit completion history
- Progress overview
- Streak tracking
- Performance insights

### Social (Planned)
- Add friends
- View friends' public habits
- Challenge friends
- Shared progress

### Widgets (Planned)
- Home screen widgets
- Quick habit completion
- Daily progress overview

### Notifications (Planned)
- Habit reminders
- Streak notifications
- Smart scheduling

---

# 📱 Screenshots

| Login | Home | Habit Details |
|-------|------|---------------|
| *Coming soon* | *Coming soon* | *Coming soon* |

---

# 🏗 Architecture

The project follows **Clean Architecture** with **MVVM**, separating UI, business logic and data layers.

```
Presentation
│
├── UI (Jetpack Compose)
├── ViewModels
│
Domain
│
├── Use Cases
├── Repository Interfaces
│
Data
│
├── Repository Implementations
├── Local Data Source
├── Remote Data Source
```

---

# 🛠 Tech Stack

### UI
- Jetpack Compose
- Material 3
- Navigation Compose

### Architecture
- MVVM
- Clean Architecture
- Repository Pattern

### Dependency Injection
- Hilt

### Asynchronous Programming
- Kotlin Coroutines
- StateFlow
- Flow

### Local Storage
- Room
- DataStore

### Backend
- Supabase
- Authentication
- PostgreSQL

### Background Work
- WorkManager

### Widgets
- Glance

### Image Loading
- Coil

### Testing
- JUnit
- MockK
- Turbine

---

# 📂 Project Structure

```
app/
├── data/
├── domain/
├── presentation/
├── di/
├── navigation/
└── ui/
```

---

# 🎯 Current Goals

- [x] Authentication
- [x] Onboarding
- [x] Modern Compose UI
- [ ] Habit CRUD
- [ ] Statistics
- [ ] Notifications
- [ ] Home screen widgets
- [ ] Social features
- [ ] Offline-first synchronization
- [ ] Wear OS support

---

# 📈 Future Improvements

- Google Sign-In
- GitHub Sign-In
- Habit templates
- Shared challenges
- Cloud synchronization
- Backup & Restore
- Dynamic widgets
- Material You customization

---

# 🤝 Contributing

Contributions, ideas and feedback are always welcome.

Feel free to open an issue or submit a pull request.

---

# 📄 License

This project is licensed under the MIT License.