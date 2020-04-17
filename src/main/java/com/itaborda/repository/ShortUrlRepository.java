package com.itaborda.repository;

import com.itaborda.model.ShortUrl;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShortUrlRepository extends MongoRepository<ShortUrl, String> {

	@Cacheable(value = ShortUrl.CACHE_NAME)
	ShortUrl findByKeyCode(String key);

	ShortUrl findByLongUrl(String url);

	@CachePut(value = ShortUrl.CACHE_NAME, key = "#shortUrl.keyCode")
	ShortUrl save(ShortUrl shortUrl);
}
