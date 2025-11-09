package com.aristidevs.habittracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shapes.kt

// Los Shapes se usan principalmente para definir las esquinas redondeadas
// por defecto de componentes como botones, tarjetas (Card), diálogos, etc.

// small     -> Suele usarse en elementos pequeños como botones, chips.
//              También se puede omitir si no lo necesitas.

// medium    -> Por defecto se usa en componentes como botones,
//              TextField o Card (si no le das un shape manual).

// large     -> Se puede asignar a contenedores más grandes,
//              como diálogos, menús o tarjetas grandes.

// extraLarge -> Para superficies grandes o diseño más "soft",
//               como fondo de una pantalla, tarjetas XL o elementos especiales.


val shapes = Shapes(
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(32.dp)
)