package com.piedrazul.backend.agenda.internal.service;

class AgendaLockConcurrencyException extends RuntimeException {
    AgendaLockConcurrencyException(Throwable cause) {
        super(cause);
    }
}
