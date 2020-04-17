package com.itaborda.model;

import com.itaborda.model.embedded.Stats;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

@Document(collection = "shorturl")
public class ShortUrl implements Serializable {

	private static final long serialVersionUID = 6529685098267757690L;

	public static final String CACHE_NAME = "shortify";

	@Id
	private String id;
	@Indexed
	private String keyCode;
	private LocalDateTime createdDate;
	private LocalDateTime lastAccessDate;
	private LocalDateTime expireDate;
	@Indexed
	private String longUrl;
	private Stats stats;


	public ShortUrl() {
	}

	public ShortUrl(String keyCode) {
		this.keyCode = keyCode;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

	public LocalDateTime getExpireDate() {
		return expireDate;
	}

	public void setExpireDate(LocalDateTime expireDate) {
		this.expireDate = expireDate;
	}

	public String getLongUrl() {
		return longUrl;
	}

	public void setLongUrl(String longUrl) {
		this.longUrl = longUrl;
	}

	public Stats getStats() {
		return stats;
	}

	public void setStats(Stats stats) {
		this.stats = stats;
	}

	public String getKeyCode() {
		return keyCode;
	}

	public void setKeyCode(String keyCode) {
		this.keyCode = keyCode;
	}

	public LocalDateTime getLastAccessDate() {
		return lastAccessDate;
	}

	public void setLastAccessDate(LocalDateTime lastAccessDate) {
		this.lastAccessDate = lastAccessDate;
	}

	public boolean isNotExpired() {

		return this.expireDate != null && this.expireDate.isAfter(LocalDateTime.now());
	}
}
