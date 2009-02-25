package com.gc.iotools.fmt.base;

public final class FormatInfo {

	private final FormatId formatId;

	private final String mimeType;

	private final String description;

	public FormatInfo(final FormatId formatId, final String mimeType,
			final String description) {
		this.formatId = formatId;
		this.mimeType = mimeType;
		this.description = description;
	}

	public FormatEnum getFormat() {
		return this.formatId.format;
	}

	public FormatId getFormatId() {
		return this.formatId;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public String getVersion() {
		return this.formatId.version;
	}

	public String getDescription() {
		return description;
	}

}
