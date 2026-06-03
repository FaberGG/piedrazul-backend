@org.springframework.modulith.ApplicationModule(
        displayName = "Pacientes",
        allowedDependencies = {
                "shared::exception",
                "shared::audit",
                "shared::util"
        })
package com.piedrazul.backend.pacientes;