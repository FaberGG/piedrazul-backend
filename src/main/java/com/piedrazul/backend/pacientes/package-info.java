@org.springframework.modulith.ApplicationModule(
        displayName = "Pacientes",
        allowedDependencies = {
                "shared::exception",
                "shared::audit"
        })
package com.piedrazul.backend.pacientes;