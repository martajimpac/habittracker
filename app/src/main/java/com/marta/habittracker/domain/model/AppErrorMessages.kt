package com.marta.habittracker.domain.model

fun AppError.toUserMessage(): String = when (this) {
    LoginError.InvalidCredentials -> "Email o contraseña incorrectos."
    LoginError.EmailNotVerified -> "Debes confirmar tu email antes de iniciar sesión."
    RegisterError.InvalidEmail -> "Introduce un correo electrónico válido."
    RegisterError.WeakPassword -> "La contraseña debe tener al menos 8 caracteres, con letras y números."
    RegisterError.EmailAlreadyRegistered -> "Este correo ya está registrado."
    RegisterError.EmailConfirmationRequired -> "Revisa tu correo para confirmar la cuenta antes de iniciar sesión."
    AppError.Common.Network -> "Error de conexión. Comprueba tu red."
    AppError.Common.Unauthorized -> "No tienes permiso para acceder."
    AppError.Common.Unknown -> "No se pudo completar la operación. Inténtalo de nuevo."
}
