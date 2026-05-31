package com.piedrazul.backend.shared.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Normaliza campos de nombre y apellido:
 * elimina espacios extra, aplica title case y quita tildes de vocales (preserva ñ/Ñ).
 */
public final class NombreNormalizadorUtil {

    private NombreNormalizadorUtil() {}

    public static String normalizar(String valor) {
        if (valor == null) return null;

        // 1. Eliminar espacios al inicio, al final y colapsar espacios intermedios
        String limpio = valor.trim().replaceAll("\\s+", " ");

        // 2. Title case: primera letra de cada palabra en mayúscula, resto en minúscula
        String titulado = Arrays.stream(limpio.split(" "))
                .map(p -> p.isEmpty() ? p :
                        Character.toUpperCase(p.charAt(0)) + p.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));

        // 3. Quitar tildes de vocales (no afecta ñ/Ñ)
        return titulado
                .replace('á', 'a').replace('Á', 'A')
                .replace('é', 'e').replace('É', 'E')
                .replace('í', 'i').replace('Í', 'I')
                .replace('ó', 'o').replace('Ó', 'O')
                .replace('ú', 'u').replace('Ú', 'U')
                .replace('ü', 'u').replace('Ü', 'U');
    }
}
