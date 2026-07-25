package com.is.bcs.application.port.in.imports;

import com.is.bcs.application.dto.SurveyCsvImportResult;
import com.is.bcs.application.dto.ImportSurveyCsvCommand;

public interface ImportSurveyCsvUseCase {

    SurveyCsvImportResult importCsv(ImportSurveyCsvCommand command);
}
