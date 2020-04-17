package com.itaborda.service;

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
import com.itaborda.model.ShortUrl;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ShortUrlServiceTest {

	@Autowired
	private ShortUrlService service;

	@MockBean
	private WorkerStatusService workerStatusService;

	@MockBean
	private ShortUrlRepository repository;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void contextLoads() {
		MockitoAnnotations.initMocks(this);
	}

	private ShortUrl shortUtilInit() {
		ShortUrl shortUrl = new ShortUrl();
		shortUrl.setLongUrl("http://www.google.com");
		shortUrl.setCreatedDate(LocalDateTime.now());
		Stats stats = new Stats();

		BrowserStats browserStats = new BrowserStats();
		browserStats.setChrome(19L);
		stats.setBrowserStats(browserStats);
		OsStat osStat = new OsStat();
		osStat.setIos(10L);
		stats.setOsStat(osStat);
		DateStat dateStat1 = new DateStat();
		dateStat1.setDayOfYear(29);
		dateStat1.setVisits(2);
		DateStat dateStat2 = new DateStat();
		dateStat2.setDayOfYear(33);
		dateStat2.setVisits(1);

		DateStat dateStat3 = new DateStat();
		dateStat3.setDayOfYear(129);
		dateStat3.setVisits(23);

		stats.getDateStats().addAll(Arrays.asList(dateStat1, dateStat2, dateStat3));
		shortUrl.setStats(stats);
		return shortUrl;
	}

	private NewLinkDto newLinkDtoInit() {
		NewLinkDto newLinkDto = new NewLinkDto();
		newLinkDto.setLongUrl("http://www.google.com");
		return newLinkDto;
	}

	private ResolveLinkDto resolveLinkDtoInit() {
		ResolveLinkDto dto = new ResolveLinkDto();
		dto.setBrowser("chrome");
		dto.setOs("windows");
		dto.setShortUrl("b");
		dto.setWorkerId(1);
		Long key = 1L;
		return dto;
	}

	@Test
	public void should_resolveShortenedUrl_when_alreadyExists() throws KeyNotFoundException, InvalidAddressException {
		// Given
		ShortUrl shortUrl = this.shortUtilInit();
		ResolveLinkDto dto = resolveLinkDtoInit();
		when(repository.findByKeyCode(dto.getShortUrl())).thenReturn(shortUrl);


		Long previousChromStat = shortUrl.getStats().getBrowserStats().getChrome();
		Long previousWinStat = shortUrl.getStats().getOsStat().getWindows();

		// When
		ShortUrl shortenedUrl = service.resolve(dto);

		// Then
		assertThat(shortenedUrl).isNotNull();
		assertThat(shortenedUrl.getLongUrl()).isEqualTo("http://www.google.com");
		assertThat(shortUrl.getStats().getBrowserStats().getChrome()).isEqualTo(previousChromStat + 1);
		assertThat(shortUrl.getStats().getOsStat().getWindows()).isEqualTo(previousWinStat + 1);
	}


	@Test(expected = KeyNotFoundException.class)
	public void should_throwException_when_shortUrlDoesNotExist() throws KeyNotFoundException, InvalidAddressException {
		// Given
		ResolveLinkDto dto = resolveLinkDtoInit();

		// When
		ShortUrl shortenedUrl = service.resolve(dto);
	}

	@Test(expected = InvalidAddressException.class)
	public void should_throwException_when_shortUrlNotProvided() throws KeyNotFoundException, InvalidAddressException {
		// Given
		ResolveLinkDto dto = resolveLinkDtoInit();
		dto.setShortUrl(null);

		// When
		ShortUrl shortenedUrl = service.resolve(dto);

	}


	@Test
	public void should_generateShortenedUrl_when_urlIsValidAndDoesNotExist() throws MalformedURLException, UnknownHostException, KeyOverFlowException {

		// Given
		NewLinkDto dto = newLinkDtoInit();
		long key = 1L;

		when(workerStatusService.getNewKey(anyString())).thenReturn(key);

		// When
		String shortedUrl = service.shorten(dto);

		// Then
		assertThat(shortedUrl).isNotNull();
		//assertThat(shortedUrl).isEqualTo(service.getNewCode(Utility.urlNormalization(dto.getLongUrl())));
	}


	@Test(expected = MalformedURLException.class)
	public void should_throwException_when_urlIsNotValid() throws MalformedURLException, UnknownHostException, KeyOverFlowException {

		// Given
		NewLinkDto dto = newLinkDtoInit();
		dto.setLongUrl("badURL");

		// When
		String shortedUrl = service.shorten(dto);
	}


	@Test
	public void should_calculateStatistics_when_ShortUrlExist() throws KeyNotFoundException {
		// Given
		String key = "1";
		when(repository.findByKeyCode(key)).thenReturn(shortUtilInit());
		//When
		VisitStateDto dto = service.getVisitStateByKey(key);
		//Then
		assertThat(dto.getDailyAverage()).isNotNull();
		assertThat(dto.getMax()).isNotNull();
		assertThat(dto.getMin()).isNotNull();
		assertThat(dto.getTotalPerYear()).isNotNull();
		assertThat(dto.getPerMonth()).isNotNull();
		Assertions.assertThat(dto.getByBrowsers()).isNotNull();
		Assertions.assertThat(dto.getByOs()).isNotNull();
		assertThat(dto.getCode()).isEqualTo(BaseResponse.SUCCESSFUL);
		assertThat(dto.getMessage()).isEqualTo("analytics");
	}


	@Test(expected = KeyNotFoundException.class)
	public void should_throwExceptionOnStatCalculation_when_shortUrlDoesNotExist() throws KeyNotFoundException {
		// Given
		String key = "1";

		// When
		VisitStateDto dto = service.getVisitStateByKey(key);

	}


}
