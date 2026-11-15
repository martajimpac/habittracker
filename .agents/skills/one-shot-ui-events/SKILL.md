---
name: one-shot-ui-events
description: Enforces a consistent approach to handling one-shot UI events in Jetpack Compose, such as navigation, Snackbars, focus changes, scrolling and other transient UI actions. Use this skill whenever implementing or modifying ViewModel-to-UI events.
---

# One-shot UI Events

## Purpose

Use a consistent event-driven approach for handling one-shot UI events in Jetpack Compose.

One-shot events represent actions that should happen once in response to a user interaction or business operation. They are not persistent UI state.

Examples include:

- Showing a Snackbar or Toast.
- Navigating to another screen.
- Requesting focus on a specific field.
- Scrolling to a particular item or position.
- Bringing a component into view.
- Opening a dialog or bottom sheet as a transient effect.
- Triggering other UI side effects.

**Always use `ObserveAsEvents` to collect one-shot UI events from a Flow.**

Do not use `StateFlow` or persistent UI state to represent transient actions.

---

## 1. ObserveAsEvents utility

Use the following reusable Composable utility to observe one-shot events while respecting the lifecycle of the current screen.

```kotlin
package com.marta.habittracker.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    onEvent: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(flow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}
```

### Important implementation details

- `LaunchedEffect` manages the collection within the Composable lifecycle.
- `repeatOnLifecycle(Lifecycle.State.STARTED)` ensures events are collected only while the screen is active.
- Collection is cancelled when the lifecycle falls below `STARTED` and restarted when it becomes active again.
- `Dispatchers.Main.immediate` ensures event handling runs on the main thread and avoids unnecessary dispatching when already on it.

**Important:** `Dispatchers.Main.immediate` does not guarantee event delivery by itself. The event Flow must also have appropriate buffering semantics.

For one-shot events emitted by a ViewModel, prefer a `Channel` exposed as a `Flow` using `receiveAsFlow()`.

---

## 2. ViewModel: emitting one-shot events

Define a dedicated event type for each screen.

Example:

```kotlin
sealed interface AddContentEvent {
    data class ShowSnackbar(val message: String) : AddContentEvent
    data class NavigateToContent(val contentId: String) : AddContentEvent
    data class FocusField(val field: AddContentField) : AddContentEvent
}
```

Create a private Channel in the ViewModel and expose it as a read-only Flow:

```kotlin
private val _events = Channel<AddContentEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()
```

Emit events from ViewModel operations:

```kotlin
viewModelScope.launch {
    _events.send(
        AddContentEvent.ShowSnackbar("Content saved successfully")
    )
}
```

For navigation:

```kotlin
viewModelScope.launch {
    _events.send(
        AddContentEvent.NavigateToContent(contentId)
    )
}
```

For focus changes:

```kotlin
viewModelScope.launch {
    _events.send(
        AddContentEvent.FocusField(AddContentField.Name)
    )
}
```

Do not expose the mutable Channel or allow the UI to emit ViewModel events directly.

---

## 3. Composable: observing events

Collect events in the screen Composable using `ObserveAsEvents`.

Example:

```kotlin
@Composable
fun AddContentScreen(
    viewModel: AddContentViewModel,
    navController: NavController,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is AddContentEvent.ShowSnackbar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }

            is AddContentEvent.NavigateToContent -> {
                navController.navigate(
                    "content/${event.contentId}"
                )
            }

            is AddContentEvent.FocusField -> {
                // Handle focus events using the corresponding
                // FocusRequester and BringIntoViewRequester.
            }
        }
    }

    // Screen UI...
}
```

### Rules

- Navigation must be handled by the UI layer, not directly by the ViewModel.
- Snackbar display must use `SnackbarHostState.showSnackbar()`.
- Suspend operations must be launched in an appropriate CoroutineScope.
- Use `rememberCoroutineScope()` when launching UI operations from the event callback.
- Do not collect the same event Flow multiple times unnecessarily.

---

## 4. Handling focus and scrolling events

For focus changes, use the same event-observation mechanism.

Example:

```kotlin
ObserveAsEvents(flow = viewModel.focusField) { field ->
    focusScope.launch {
        when (field) {
            AddContentField.Name -> {
                nameBringIntoView.bringIntoView()
                nameFocus.requestFocus()
            }

            AddContentField.Days -> {
                daysBringIntoView.bringIntoView()
                daysFocus.requestFocus()
            }

            AddContentField.Reminder -> {
                reminderBringIntoView.bringIntoView()
                reminderFocus.requestFocus()
            }
        }
    }
}
```

Use `BringIntoViewRequester` and `FocusRequester` to ensure the requested field becomes visible and receives focus.

Do not put focus or scrolling side effects directly inside the UI state rendering logic.

---

## 5. State versus Events

Always distinguish between persistent UI state and one-shot UI events.

### UI State

Use `StateFlow` for information that represents the current state of the screen and must be rendered consistently.

Examples:

- Loading indicators.
- Form values.
- Validation errors.
- Selected items.
- Lists of content.
- Empty states.
- Whether a button is enabled.

Example:

```kotlin
data class AddContentUiState(
    val title: String = "",
    val isLoading: Boolean = false,
    val isSaveEnabled: Boolean = false,
)
```

### One-shot UI Events

Use a dedicated event Flow for actions that should be executed in response to a particular occurrence.

Examples:

- Navigate to another screen.
- Show a Snackbar.
- Request focus.
- Scroll to an item.
- Display a transient notification.

**Never model navigation or Snackbar messages as persistent UI state just to trigger an action.**

---

## 6. Mandatory architectural rules

Whenever implementing a feature involving one-shot UI actions:

1. Define a dedicated event type when multiple event types belong to the same screen.
2. Emit events from the ViewModel through a private buffered Channel.
3. Expose events as a read-only Flow using `receiveAsFlow()`.
4. Collect events in the screen Composable using `ObserveAsEvents`.
5. Execute UI side effects in the UI layer.
6. Use `rememberCoroutineScope()` for suspend UI operations triggered by events.
7. Keep persistent screen information in `StateFlow` and transient actions in event Flows.
8. Avoid duplicating event collectors for the same screen.
9. Do not use `LaunchedEffect` with individual event values as a replacement for the event-observation mechanism.
10. Follow the existing project architecture and naming conventions.

### Delivery guarantees

A buffered Channel helps retain events while the UI collector is temporarily stopped, provided its buffer does not overflow and the ViewModel remains alive.

Do not assume that one-shot events survive process death. If an action must be restored after process recreation, model the necessary information as persistent state instead.

---

## Final principle

**State describes what the UI should display. Events describe what the UI should do.**

Whenever a feature needs to trigger a one-time UI action, use the `ObserveAsEvents` pattern consistently.
