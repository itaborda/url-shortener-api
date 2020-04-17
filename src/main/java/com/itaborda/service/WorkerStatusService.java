package com.itaborda.service;

import com.itaborda.exception.KeyOverFlowException;

public interface WorkerStatusService {

    Long getNewKey(String workerId) throws KeyOverFlowException;
}
