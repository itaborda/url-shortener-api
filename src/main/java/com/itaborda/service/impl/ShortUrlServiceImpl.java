package com.itaborda.service.impl;

import com.itaborda.controller.dto.BaseResponse;
import com.itaborda.controller.dto.NewLinkDto;
import com.itaborda.controller.dto.ResolveLinkDto;
import com.itaborda.controller.dto.VisitStateDto;
import com.itaborda.exception.InvalidAddressException;
import com.itaborda.exception.KeyNotFoundException;
import com.itaborda.exception.KeyOverFlowException;
import com.itaborda.model.embedded.BrowserStats;
import com.itaborda.model.embedded.DateStat;
import com.itaborda.model.embedded.OsStat;
import com.itaborda.model.embedded.Stats;
import com.itaborda.repository.ShortUrlRepository;
import com.itaborda.service.ShortUrlService;
import com.itaborda.service.WorkerStatusService;
import com.itaborda.util.Base58;
import com.itaborda.util.Utility;
import com.itaborda.model.ShortUrl;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Service for shortening , expanding and providing statistics
 *
 * @author MohammadReza Alagheband
 */
@Service
public class ShortUrlServiceImpl implements ShortUrlService {
	private ShortUrlRepository shortUrlRepository;
	private WorkerStatusService workerStatusService;

	@Value("${spring.cache.redis.time-to-live}")
	private String ttl;

	public ShortUrlServiceImpl(ShortUrlRepository shortUrlRepository, WorkerStatusService workerStatusService) {
		this.shortUrlRepository = shortUrlRepository;
		this.workerStatusService = workerStatusService;
	}

	/**
	 * the service that provides shortening url
	 * for shortening purpose, workerID (docker Container instance hostname) should be extracted
	 * and based on that a new code is used to be encoded in base58.
	 * after code assignement , created date , stats are also initilized.
	 *
	 * @param linkDto a container to hold the provided long url
	 * @return short url generated based on base58 encoding mechanism
	 * @throws MalformedURLException if the format of the provided url is not valid
	 * @throws UnknownHostException  if the system requesting, does not have proper hostname
	 * @throws KeyOverFlowException  if the system has exhausted the maximum amount of counters
	 */
	@Override
	public String shorten(NewLinkDto linkDto) throws UnknownHostException, MalformedURLException, KeyOverFlowException {

		linkDto.setLongUrl(Utility.urlNormalization(linkDto.getLongUrl()));

		if (!Utility.isUrlValid(linkDto.getLongUrl())) throw new MalformedURLException();

		Optional<ShortUrl> existingShortUrl = Optional.ofNullable(shortUrlRepository.findByLongUrl(linkDto.getLongUrl()))
				.filter(s -> s.isNotExpired());

		if (existingShortUrl.isPresent()) return existingShortUrl.get().getKeyCode();

		ShortUrl newShortUrl = new ShortUrl();
		newShortUrl.setKeyCode(getNewCode(linkDto.getLongUrl()));
		newShortUrl.setLongUrl(linkDto.getLongUrl());
		newShortUrl.setStats(this.initState());
		newShortUrl.setCreatedDate(LocalDateTime.now());
		newShortUrl.setExpireDate(newShortUrl.getCreatedDate().plus(Duration.parse(ttl)));

		shortUrlRepository.save(newShortUrl);
		return newShortUrl.getKeyCode();
	}


	/**
	 * Generetate new code based sha1Hex + workerStatusService new Key
	 *
	 * @param longUrl a container to hold the provided long url
	 * @return short url generated based on base58 encoding mechanism
	 * @throws UnknownHostException if the system requesting, does not have proper hostname
	 * @throws KeyOverFlowException if the system has exhausted the maximum amount of counters
	 * @see WorkerStatusService#getNewKey(String)
	 */
	@Override
	public String getNewCode(String longUrl) throws UnknownHostException, KeyOverFlowException {

		String workerID = Utility.getHostname();
		Long newKey = workerStatusService.getNewKey(workerID);

		String hash = DigestUtils.sha1Hex(longUrl);
		int pos = new Random().nextInt(hash.length() - 2);

		return Base58.fromBase10(newKey) + hash.substring(pos, pos + 2);
	}

	/**
	 * once shortUrl is provided , this method will return corresponding url by decoding and converting in back
	 * to decimal system and finding appropriate long url with that decimal key in the DB
	 *
	 * @param dto container that holds requesting browser , os and shortUrl
	 * @return founded ShortUrl Entity matching the given in the dto shortUrl.
	 * @throws KeyNotFoundException    if the provided shortUrl is not available in the DB
	 * @throws InvalidAddressException if the key (or short url) is empty or null
	 */
	@Override
	public ShortUrl resolve(ResolveLinkDto dto) throws KeyNotFoundException, InvalidAddressException {

		if (dto.getShortUrl() == null || "".equals(dto.getShortUrl())) throw new InvalidAddressException();

		ShortUrl shortUrl = Optional.ofNullable(shortUrlRepository.findByKeyCode(dto.getShortUrl()))
				.filter(c -> c.isNotExpired())
				.orElseThrow(KeyNotFoundException::new);

		this.updateStats(dto, shortUrl);
		shortUrl.setLastAccessDate(LocalDateTime.now());
		shortUrlRepository.save(shortUrl);

		return shortUrl;
	}


	/**
	 * give the shortUrl generated code, statistics is calculated and will be returned
	 *
	 * @param key give the shortUrl generated code,
	 * @return analytics information for the give shortUrl generated code in the VisitStateDto Obj
	 * @throws KeyNotFoundException if the provided shortUrl is not available in the DB
	 */
	public VisitStateDto getVisitStateByKey(String key) throws KeyNotFoundException {
		VisitStateDto dto = new VisitStateDto();
		ShortUrl shortUrl = Optional.ofNullable(shortUrlRepository.findByKeyCode(key))
				.map(c -> c)
				.orElseThrow(KeyNotFoundException::new);

		LongSummaryStatistics longSummaryStatistics = shortUrl.getStats().getDateStats().stream().mapToLong(d -> d.getVisits()).summaryStatistics();
		dto.setDailyAverage(longSummaryStatistics.getAverage());
		dto.setMax(longSummaryStatistics.getMax());
		dto.setMin(longSummaryStatistics.getMin());
		dto.setTotalPerYear(longSummaryStatistics.getSum());
		dto.setPerMonth(getMonthlyVisitReport(shortUrl));
		dto.setByOs(shortUrl.getStats().getOsStat());
		dto.setByBrowsers(shortUrl.getStats().getBrowserStats());
		dto.setLastAccessDate(shortUrl.getLastAccessDate());
		dto.setCode(BaseResponse.SUCCESSFUL);
		dto.setSuccess(true);
		dto.setMessage("analytics");

		return dto;
	}

	/**
	 * once expanding request, the stats such as browser and os requesting , nth day of year request has come are updated
	 *
	 * @param dto      container that holds requesting browser , os and shortUrl
	 * @param shortUrl founded shortUrl from DB , to be updated
	 * @return stats updated obj of the requested shortURL
	 */
	private ShortUrl updateStats(ResolveLinkDto dto, ShortUrl shortUrl) {
		switch (dto.getBrowser().toLowerCase()) {
			case "internet explorer":
				shortUrl.getStats().getBrowserStats().incrementIe();
				break;
			case "safari":
				shortUrl.getStats().getBrowserStats().incrementSafari();
				break;
			case "chrome":
				shortUrl.getStats().getBrowserStats().incrementChrome();
				break;
			case "firefox":
				shortUrl.getStats().getBrowserStats().incrementFireFox();
				break;
			case "opera":
				shortUrl.getStats().getBrowserStats().incrementOpera();
				break;

			default:
				shortUrl.getStats().getBrowserStats().incrementOthers();
				break;
		}

		switch (dto.getOs().toLowerCase()) {
			case "windows":
				shortUrl.getStats().getOsStat().incrementWindows();
				break;
			case "mac_os":
				shortUrl.getStats().getOsStat().incrementMacOs();
				break;
			case "linux":
				shortUrl.getStats().getOsStat().incrementLinux();
				break;
			case "android":
				shortUrl.getStats().getOsStat().incrementAndroid();
				break;
			case "iphone":
				shortUrl.getStats().getOsStat().incrementIos();
				break;

			default:
				shortUrl.getStats().getOsStat().incrementOthers();
				break;
		}

		Calendar calendar = Calendar.getInstance();
		int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

		shortUrl.getStats().getDateStats().stream().filter((d) -> d.getDayOfYear() == dayOfYear).findFirst().map(d -> {
			d.incrementVisit();
			return d;
		}).orElseGet(() -> {
			DateStat newDateStat = new DateStat(dayOfYear, 1);
			shortUrl.getStats().getDateStats().add(newDateStat);
			return newDateStat;
		});

		return shortUrl;
	}

	/**
	 * stats initialized once new shortening request comes to service
	 *
	 * @return initialized Stats
	 */
	private Stats initState() {
		Stats state = new Stats();
		state.setBrowserStats(new BrowserStats());
		state.setOsStat(new OsStat());
		return state;
	}

	/**
	 * request are recorded as the nth day of the year in the DateStats,so this methos loops through the each month
	 * and calculates total visit of each month by summing during that month.
	 *
	 * @param shortUrl founded shortUrl from DB for which date are going to be calculated monthly
	 * @return a map of that key represent month name, and value is equal to the sum of visits in that month
	 */
	private Map<String, Long> getMonthlyVisitReport(ShortUrl shortUrl) {
		int year = LocalDate.now().getYear();
		Map<String, Long> monthlyVisitsReport = new HashMap<>();
		for (LocalDate date = LocalDate.of(year, 1, 1); date.isBefore(LocalDate.of(year + 1, 1, 1)); date = date.plusMonths(1)) {
			String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
			Long totalVisits = this.getTotalVisitForAMonth(shortUrl, date.getDayOfYear(), date.plusMonths(1).getDayOfYear());
			monthlyVisitsReport.put(month, totalVisits);
		}

		return monthlyVisitsReport;
	}

	/**
	 * a helper to calculate the sum of visit during a date range by startDay number of year to endDay number in the year
	 *
	 * @param shortUrl founded shortUrl from DB for which date are going to be calculated monthly
	 * @param startDay start nth day of the range
	 * @param lastDay  end nth day of the range
	 * @return sum of visits in the duration
	 */
	private Long getTotalVisitForAMonth(ShortUrl shortUrl, Integer startDay, Integer lastDay) {
		return shortUrl.getStats().getDateStats().stream().filter(d -> d.getDayOfYear() >= startDay && d.getDayOfYear() < lastDay).mapToLong(d -> d.getVisits()).sum();
	}

}
