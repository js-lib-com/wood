package com.jslib.wood.preview;

import com.jslib.wood.lang.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.BlockingQueue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EventsManagerTest {
	private EventsManager manager;

	@Before
	public void beforeTest() {
		manager = new EventsManager();
	}

	@Test
	public void GivenNotRegisteredClientID_WhenAcquireQueue_ThenCreateQueueOnTheFly() {
		// GIVEN
		int clientID = 1;

		// WHEN
		BlockingQueue<Event> queue = manager.acquireQueue(clientID);

		// THEN
		assertThat(queue, notNullValue());
		assertThat(manager.getQueues().get(1), notNullValue());
		assertThat(manager.getQueues().get(1), equalTo(queue));
	}

	@Test(expected = IllegalStateException.class)
	public void GivenAlreadyCreatedQueue_WhenAcquireQueue_ThenIllegalStateException() {
		// GIVEN
		@SuppressWarnings("unchecked")
		BlockingQueue<Event> queue = mock(BlockingQueue.class);
		manager.getQueues().put(1, queue);

		// WHEN
		manager.acquireQueue(1);

		// THEN
	}

	@Test
	public void GivenCreatedQueue_WhenReleaseQueue_ThenQueueRemoved() {
		// GIVEN
		@SuppressWarnings("unchecked")
		BlockingQueue<Event> queue = mock(BlockingQueue.class);
		manager.getQueues().put(1, queue);

		// WHEN
		manager.releaseQueue(1);

		// THEN
		assertThat(manager.getQueues().get(1), nullValue());
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMissingClientI_WhenReleaseQueue_ThenIllegalStateException() {
		// GIVEN
		int clientID = 1;

		// WHEN
		manager.releaseQueue(clientID);

		// THEN
	}

	@Test
	public void GivenQueueWithAvailableSpace_WhenPushEvent_ThenQueueOfferInvoked() {
		// GIVEN
		@SuppressWarnings("unchecked")
		BlockingQueue<Event> queue = mock(BlockingQueue.class);
		// blocking queue returns true if it has space and adding new item succeed
		when(queue.offer(any(Event.class))).thenReturn(true);
		manager.getQueues().put(1, queue);

		// WHEN
		Event event = new KeepAliveEvent();
		manager.pushEvent(event);

		// THEN
		verify(queue, times(1)).offer(event);
	}

	@Test
	public void GiveQueueWithoutSpace_WhenPushEvent_ThenEventLostAndLogError() {
		// GIVEN
		@SuppressWarnings("unchecked")
		BlockingQueue<Event> queue = mock(BlockingQueue.class);
		// blocking queue returns false if it has no space and adding new item fails
		when(queue.offer(any(Event.class))).thenReturn(false);
		manager.getQueues().put(1, queue);

		// WHEN
		Event event = new KeepAliveEvent();
		manager.pushEvent(event);

		// THEN
		verify(queue, times(1)).offer(event);
	}
}
