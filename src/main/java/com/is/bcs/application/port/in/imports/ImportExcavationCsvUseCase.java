package com.is.bcs.application.port.in.imports;

import com.is.bcs.application.dto.ExcavationImportResult;
import com.is.bcs.application.dto.ImportExcavationCsvCommand;

public interface ImportExcavationCsvUseCase {

    ExcavationImportResult importCsv(ImportExcavationCsvCommand command);
}
