package com.itaborda.service;

import com.itaborda.repository.AllocatedRangePartitionStatusRepository;
import com.itaborda.service.impl.AllocatedRangePartitionStatusServiceImpl;
import com.itaborda.model.AllocatedRangePartitionStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AllocatedRangePartitionStatusServiceTest {

	@Autowired
	private AllocatedRangePartitionStatusServiceImpl service;

	@MockBean
	private AllocatedRangePartitionStatusRepository repository;


	@Before
	public void contextLoads() {
		MockitoAnnotations.initMocks(this);
	}


	@Test
	public void should_rangeCounterShouldIncreased_when_allocationRangeRequest() {

		//Given
		AllocatedRangePartitionStatus allocatedRangePartitionStatus = new AllocatedRangePartitionStatus();
		allocatedRangePartitionStatus.setAllocatedPartitionNumber(1);
		Integer lastAllocatedPartition = allocatedRangePartitionStatus.getAllocatedPartitionNumber();
		when(repository.findAll()).thenReturn(Arrays.asList(allocatedRangePartitionStatus));

		//When
		Integer newRangePartition = service.allocateRangePartition();

		//Then
		assertThat(newRangePartition).isNotNull();
		assertThat(newRangePartition).isEqualTo(lastAllocatedPartition + 1);
	}


	@Test
	public void should_rangeShouldBeCreated_when_noRangePartitionAlreadyInDB() {

		//Given

		//When
		Integer newRangePartition = service.allocateRangePartition();

		//Then
		assertThat(newRangePartition).isNotNull();
		assertThat(newRangePartition).isEqualTo(1);
	}

}
