package js.wood;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.lang.Event;
import js.lang.KeepAliveEvent;

@RunWith(MockitoJUnitRunner.class)
public class EventsManagerTest {
	@Mock
	private Map<Integer, BlockingQueue<Event>> queues;

	private EventsManager manager;

	@Before
	public void beforeTest() {
		manager = new EventsManager();
	}

	@Test
	public void instance() {
		EventsManager instance1 = EventsManager.instance();
		EventsManager instance2 = EventsManager.instance();
		assertThat(instance1, equalTo(instance2));
	}

	@Test
	public void acquireQueue() {
		BlockingQueue<Event> queue = manager.acquireQueue(1);
		assertThat(queue, notNullValue());
		assertThat(manager.getQueues().get(1), notNullValue());
		assertThat(manager.getQueues().get(1), equalTo(queue));
	}

	@Test(expected = BugError.class)
	public void acquireQueue_AlreadyCreated() {
		@SuppressWarnings("unchecked")
		BlockingQueue<Event> queue = mock(BlockingQueue.class);
		manager.getQueues().put(1, queue);

		manager.acquireQueue(1);
	}

	@Test
	public void releaseQueue() {
		@SuppressWarnings("unchecked")
		BlockingQueue<Event> queue = mock(BlockingQueue.class);
		manager.getQueues().put(1, queue);

		manager.releaseQueue(1);
		assertThat(manager.getQueues().get(1), nullValue());
	}

	@Test(expected = BugError.class)
	public void releaseQueue_Missing() {
		manager.releaseQueue(1);
	}

	@Test
	public void pushEvent() {
		@SuppressWarnings("unchecked")
		BlockingQueue<Event> queue = mock(BlockingQueue.class);
		when(queue.offer(any(Event.class))).thenReturn(true);
		manager.getQueues().put(1, queue);

		Event event = new KeepAliveEvent();
		manager.pushEvent(event);
		verify(queue, times(1)).offer(event);
	}

	@Test
	public void pushEvent_QueueFull() {
		@SuppressWarnings("unchecked")
		BlockingQueue<Event> queue = mock(BlockingQueue.class);
		manager.getQueues().put(1, queue);

		Event event = new KeepAliveEvent();
		manager.pushEvent(event);
		verify(queue, times(1)).offer(event);
	}
}
