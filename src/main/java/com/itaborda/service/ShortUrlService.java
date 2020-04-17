package com.itaborda.service;

import com.itaborda.controller.dto.NewLinkDto;
import com.itaborda.controller.dto.ResolveLinkDto;
import com.itaborda.controller.dto.VisitStateDto;
import com.itaborda.exception.InvalidAddressException;
import com.itaborda.exception.KeyNotFoundException;
import com.itaborda.exception.KeyOverFlowException;
import com.itaborda.model.ShortUrl;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

public interface ShortUrlService {

    String shorten(NewLinkDto linkDto) throws MalformedURLException, UnknownHostException, KeyOverFlowException;

	String getNewCode(String longUrl) throws UnknownHostException, KeyOverFlowException;

	ShortUrl resolve(ResolveLinkDto dto) throws KeyNotFoundException, InvalidAddressException;

    VisitStateDto getVisitStateByKey(String key) throws KeyNotFoundException;

}
