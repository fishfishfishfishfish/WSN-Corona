package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import junit.framework.TestCase;
import au.edu.usyd.corona.middleLayer.Network;

public class SchedulerTest extends TestCase {
	private DestroyableSchedular s;
	
	@Override
	protected void setUp() {
		Network.initialize(Network.MODE_UNITTEST);
	}
	
	public void testNormal() {
		DestroyableSchedular.initialize();
		s = (DestroyableSchedular) DestroyableSchedular.getInstance();
		assertEquals(0, s.size());
		
		// test an add works
		s.addTask(new TestTask(0, System.currentTimeMillis(), 5000, 100));
		assertEquals(1, s.size());
		assertTrue(s.containsQuery(0));
		
		// test a remove works for a task already in the scheduler
		s.killQuery(0);
		assertEquals(0, s.size());
		assertFalse(s.containsQuery(0));
		
		// test a remove works for a task that is not in the scheduler
		s.killQuery(1);
		s.killQuery(Integer.MIN_VALUE);
		assertEquals(0, s.size());
		
		DestroyableSchedular.destroyInstance();
	}
	
	public void testRescheduling() throws InterruptedException {
		DestroyableSchedular.initialize();
		s = (DestroyableSchedular) DestroyableSchedular.getInstance();
		
		// add some repeating tasks
		s.addTask(new TestTask(0, System.currentTimeMillis(), 100, 200));
		s.addTask(new TestTask(1, System.currentTimeMillis(), 1000, 5));
		s.addTask(new TestTask(2, System.currentTimeMillis(), 5000, 2));
		assertEquals(3, s.size());
		
		Thread.sleep(6000);
		assertEquals(1, s.size());
		assertTrue(s.containsQuery(0));
		
		Thread.sleep(5000);
		assertEquals(1, s.size());
		
		Thread.sleep(10000);
		assertEquals(0, s.size());
		
		DestroyableSchedular.destroyInstance();
	}
	
	private static class DestroyableSchedular extends Scheduler {
		public static void initialize() {
			if (instance == null)
				(thread = new Thread(instance = new DestroyableSchedular(), "Scheduler")).start();
		}
		
		@SuppressWarnings("deprecation")
		private static void destroyInstance() {
			thread.stop();
			thread = null;
			instance = null;
		}
	}
	
	private static class TestTask extends SchedulableTask {
		public TestTask(int queryId, long firstExecutionTime, long reschedulePeriod, int runCountTotal) {
			super(new TaskID(queryId), firstExecutionTime, reschedulePeriod, runCountTotal);
		}
		
		@Override
		protected void _decode(DataInput data) throws IOException {
		}
		
		@Override
		protected void _deconstruct() {
		}
		
		@Override
		protected void _encode(DataOutput data) throws IOException {
		}
		
		@Override
		protected void _execute() {
		}
		
		@Override
		protected void _reschedule() {
		}
		
		@Override
		public void baseInit() throws IOException {
		}
		
		@Override
		public void nodeInit() {
		}
	}
}
