package com.devoxx.data.register;

public class DefaultExtractor extends BaseExtractor {
	private String[] dataParts;

	public DefaultExtractor(String data) {
		dataParts = data.split(",");
	}

	@Override protected String userName() {
		return extractIfExists(dataParts, 1);
	}

	@Override protected String userSurname() {
		return extractIfExists(dataParts, 2);
	}

	@Override protected String userCompany() {
		return extractIfExists(dataParts, 3);
	}

	@Override protected String userJob() {
		return extractIfExists(dataParts, 5);
	}

	@Override protected String userId() {
		return extractIfExists(dataParts, 0);
	}

	private String extractIfExists(String[] array, int index) {
		return index >= array.length ? "" : array[index];
	}
}
