package com.piedrazul.backend.reportes.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportResult {
    byte[] data;
    String contentType;
}
