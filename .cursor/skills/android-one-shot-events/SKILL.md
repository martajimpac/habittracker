---
name: android-one-shot-events
description: >-
  YOU MUST use this whenever implementing or changing Android Compose ViewModels,
  Screens, navigation effects, dialogs, snackbars/toasts, or any one-shot UI event.
  Enforces Channel(BUFFERED) + receiveAsFlow() + ObserveAsEvents — never sticky
  StateFlow flags, SharedFlow, CollectAsEffect, or collectAsState* for events.
---

# Android one-shot UI events (Channel + ObserveAsEvents)

<HARD-GATE>
Before writing or editing ViewModel/Screen code that navigates, shows a dialog/snackbar/toast, opens another screen, or otherwise fires a one-time UI effect: follow this skill exactly. Do not use SharedFlow, CollectAsEffect, or sticky UiState flags for those effects.
</HARD-GATE>

When implementing one-shot UI events in this project, **always use the `Channel` + `ObserveAsEvents` pattern**.

Do NOT use `StateFlow` for one-shot events such as:

* Showing dialogs
* Navigation events
* Toasts / snackbars
* Opening another screen
* Any event that should be handled only once

## ViewModel

Events must be exposed through a `Channel` and converted to a `Flow` using `receiveAsFlow()`.

For example:

```kotlin
private val _events = Channel<UiEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()

private suspend fun sendEvent(event: UiEvent) {
    _events.send(event)
}
```

Use a sealed interface/class for the events:

```kotlin
sealed interface UiEvent {
    data object ShowSuccessDialog : UiEvent
    data class ShowError(val message: String) : UiEvent
    data object NavigateBack : UiEvent
}
```

When an event needs to be emitted:

```kotlin
viewModelScope.launch {
    _events.send(UiEvent.ShowSuccessDialog)
}
```

## Compose

In every Composable that needs to observe one-shot events, use the following `ObserveAsEvents` helper:

```kotlin
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    onEvent: (T) -> Unit
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

Prefer the project helper at `presentation/utils/ObserveAsEvents.kt` when present. If missing, create it (public, not `private` per screen).

Then observe events like this:

```kotlin
ObserveAsEvents(
    flow = viewModel.events
) { event ->
    when (event) {
        UiEvent.ShowSuccessDialog -> {
            // Show dialog
        }

        is UiEvent.ShowError -> {
            // Show error
        }

        UiEvent.NavigateBack -> {
            // Navigate back
        }
    }
}
```

## Important rules

1. **Use `StateFlow` only for persistent UI state.**
2. **Use `Channel` for one-shot events.**
3. Expose the `Channel` as a `Flow` with `receiveAsFlow()`.
4. Always collect these event flows using the `ObserveAsEvents()` helper above.
5. Do NOT reset a `StateFlow` to `null` after consuming an event.
6. Do NOT put one-shot events inside the UI state.
7. Do NOT collect these event flows directly with `collectAsState()` or `collectAsStateWithLifecycle()`, because events are not UI state.
8. The `Channel` should use `Channel.BUFFERED`.
9. Keep the event handling lifecycle-aware using `repeatOnLifecycle(Lifecycle.State.STARTED)`.
10. Use `Dispatchers.Main.immediate` as shown in the `ObserveAsEvents` implementation.

The goal is to ensure that one-shot events are handled as events rather than being re-emitted when the Composable recomposes or the user navigates back to the screen.

## Forbidden (never ship)

```kotlin
// BAD — sticky state used as a one-shot event
data class LoginUiState(val isUserLogged: Boolean = false)

LaunchedEffect(uiState.isUserLogged) {
    if (uiState.isUserLogged) navigateToHome()
}

// BAD — SharedFlow / CollectAsEffect for new one-shot UI events
private val _navigateToHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
CollectAsEffect(viewModel.navigateToHome) { navigateToHome() }
```

## Checklist before finishing a Screen/ViewModel change

- [ ] Persistent UI is `StateFlow` / `UiState` only
- [ ] One-shot effects use `Channel(Channel.BUFFERED)` + `receiveAsFlow()`
- [ ] Compose uses `ObserveAsEvents` (not `collectAsState*`, not sticky flags)
- [ ] No new `SharedFlow` / `CollectAsEffect` for UI events
