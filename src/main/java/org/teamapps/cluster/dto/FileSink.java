package org.teamapps.cluster.dto;

import java.io.File;
import java.io.IOException;

public interface FileSink {

	String handleFile(File file) throws IOException;
}
