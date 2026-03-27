@org.springframework.modulith.ApplicationModule(
        displayName = "Medicos",
        allowedDependencies = {
                "auth::api",
                "shared::exception",
                "shared::audit"
        })
package com.piedrazul.backend.medicos;