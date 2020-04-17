package com.itaborda.service;


import com.itaborda.exception.KeyOverFlowException;
import com.itaborda.model.embedded.AllocatedCounter;
import com.itaborda.repository.WorkerStatusRepository;
import com.itaborda.service.impl.WorkerStatusServiceImpl;
import com.itaborda.model.WorkerStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkerStatusServiceTest {

	@Autowired
	private WorkerStatusServiceImpl service;

	@MockBean
	private WorkerStatusRepository workerStatusRepository;
	@MockBean
	private AllocatedRangePartitionStatusService allocatedRangePartitionStatusService;

	@Before
	public void contextLoads() {
		MockitoAnnotations.initMocks(this);
	}

	private WorkerStatus initEastingScenarioModel() {
		AllocatedCounter allocatedCounter = new AllocatedCounter();
		allocatedCounter.setExhausted(false);
		allocatedCounter.setCounter(19999998L);
		allocatedCounter.setRangeNumber(1);

		WorkerStatus workerStatus = new WorkerStatus("33cc6eebd387");
		workerStatus.getAllocatedRanges().add(allocatedCounter);

		return workerStatus;
	}

	private WorkerStatus initNewRangeAllocationScenarioModel() {
		AllocatedCounter allocatedCounter = new AllocatedCounter();
		allocatedCounter.setExhausted(true);
		allocatedCounter.setCounter(19999999L);
		allocatedCounter.setRangeNumber(1);

		WorkerStatus workerStatus = new WorkerStatus("33cc6eebd387");
		workerStatus.getAllocatedRanges().add(allocatedCounter);

		return workerStatus;
	}

	@Test
	public void should_generateDecimalID_when_urlIsValidAndDoesNotExist() throws KeyOverFlowException {

		//Given
		String workerId = "33cc6eebd387";
		Integer allocatedRangePartition = 1;
		when(allocatedRangePartitionStatusService.allocateRangePartition()).thenReturn(allocatedRangePartition);

		//When
		Long allocatedDecimalID = service.getNewKey(workerId);

		//Then
		assertThat(allocatedDecimalID).isEqualTo(1L);
	}

	@Test
	public void should_ExhaustAFilledRange_when_theLastAllocationRequest() throws KeyOverFlowException {

		//Given
		WorkerStatus workerStatus = initEastingScenarioModel();
		when(workerStatusRepository.findByWorkerId(workerStatus.getWorkerId())).thenReturn(workerStatus);

		//When
		Long allocatedDecimalID = service.getNewKey(workerStatus.getWorkerId());

		//Then
		assertThat(allocatedDecimalID).isNotNull();
		assertThat(workerStatus.getAllocatedRanges().get(0).getExhausted()).isEqualTo(true);
	}

	@Test
	public void should_AllocateNewRange_when_AllOtherRangesAlreadyExhauted() throws KeyOverFlowException {

		//Given
		WorkerStatus workerStatus = initNewRangeAllocationScenarioModel();
		Integer alreadyAllocatedRangeSize = workerStatus.getAllocatedRanges().size();
		Integer allocatedRangePartition = 2;
		when(workerStatusRepository.findByWorkerId(workerStatus.getWorkerId())).thenReturn(workerStatus);
		when(allocatedRangePartitionStatusService.allocateRangePartition()).thenReturn(allocatedRangePartition);

		//When
		Long allocatedDecimalID = service.getNewKey(workerStatus.getWorkerId());

		//Then
		assertThat(allocatedDecimalID).isNotNull();
		assertThat(workerStatus.getAllocatedRanges().size()).isEqualTo(alreadyAllocatedRangeSize + 1);
	}


}
