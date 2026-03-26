@org.springframework.modulith.ApplicationModule(
        displayName = "Pacientes",
        allowedDependencies = {
                "auth::api",
                "shared::exception",
                "shared::audit"
        })
package com.piedrazul.backend.pacientes;