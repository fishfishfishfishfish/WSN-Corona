package au.edu.usyd.corona.collections;


import java.util.ArrayList;

import junit.framework.TestCase;

public class PriorityBlockingQueueTest extends TestCase {
	private PriorityBlockingQueue queue;
	
	private class CInteger implements Comparable {
		private final Integer n;
		
		public CInteger(Integer n) {
			this.n = n;
		}
		
		public int compareTo(Object o) {
			return n - ((CInteger) o).getN();
		}
		
		public Integer getN() {
			return n;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof CInteger && ((CInteger) o).getN().equals(n))
				return true;
			return false;
		}
		
		@Override
		public String toString() {
			return n.toString();
		}
	}
	
	@Override
	public void setUp() {
		queue = new PriorityBlockingQueue();
	}
	
	public void testSize() {
		assertEquals(0, queue.size());
		queue.add(new CInteger(5));
		assertEquals(1, queue.size());
		CInteger minusOne = new CInteger(-1);
		queue.add(minusOne);
		assertEquals(2, queue.size());
		queue.add(new CInteger(0));
		assertEquals(3, queue.size());
		queue.add(new CInteger(100));
		assertEquals(4, queue.size());
		queue.remove(minusOne);
		assertEquals(3, queue.size());
		queue.peek();
		assertEquals(3, queue.size());
		queue.peekBlocking();
		assertEquals(3, queue.size());
		queue.poll();
		assertEquals(2, queue.size());
		queue.pollBlocking();
		assertEquals(1, queue.size());
		queue.poll();
		assertEquals(0, queue.size());
		queue.poll();
		assertEquals(0, queue.size());
		queue.add(new CInteger(5));
		assertEquals(1, queue.size());
		queue.add(minusOne);
		assertEquals(2, queue.size());
		queue.add(new CInteger(0));
		assertEquals(3, queue.size());
		queue.clear();
		assertEquals(0, queue.size());
	}
	
	public void testClear() {
		assertEquals(0, queue.size());
		queue.clear();
		assertEquals(0, queue.size());
		
		queue.add(new CInteger(5));
		queue.add(new CInteger(-1));
		queue.add(new CInteger(0));
		queue.add(new CInteger(100));
		assertEquals(4, queue.size());
		
		queue.clear();
		assertEquals(0, queue.size());
		assertEquals(null, queue.peek());
		assertEquals(null, queue.poll());
	}
	
	public void testAddPeek() {
		assertEquals(null, queue.peek());
		queue.add(new CInteger(5));
		assertEquals(new CInteger(5), queue.peek());
		queue.add(new CInteger(-1));
		assertEquals(new CInteger(-1), queue.peek());
		queue.add(new CInteger(0));
		assertEquals(new CInteger(-1), queue.peek());
		queue.add(new CInteger(100));
		assertEquals(new CInteger(-1), queue.peek());
		queue.add(new CInteger(-5));
		assertEquals(new CInteger(-5), queue.peek());
		queue.add(new CInteger(-6));
		assertEquals(new CInteger(-6), queue.peek());
		queue.add(new CInteger(-4));
		assertEquals(new CInteger(-6), queue.peek());
	}
	
	public void testAddPoll() {
		assertEquals(null, queue.poll());
		queue.add(new CInteger(5));
		queue.add(new CInteger(-1));
		queue.add(new CInteger(0));
		queue.add(new CInteger(3));
		queue.add(new CInteger(9));
		queue.add(new CInteger(11));
		assertEquals(new CInteger(-1), queue.poll());
		assertEquals(new CInteger(0), queue.poll());
		assertEquals(new CInteger(3), queue.poll());
		queue.add(new CInteger(-22));
		queue.add(new CInteger(100));
		queue.add(new CInteger(6));
		assertEquals(new CInteger(-22), queue.poll());
		assertEquals(new CInteger(5), queue.poll());
		assertEquals(new CInteger(6), queue.poll());
		assertEquals(new CInteger(9), queue.poll());
		assertEquals(new CInteger(11), queue.poll());
		assertEquals(new CInteger(100), queue.poll());
	}
	
	public void testAddPollRandom() {
		int count = 1500;
		for (int i = 0; i < count; i++) {
			queue.add(new CInteger((int) (Math.random() * 1000)));
		}
		int j = 0;
		CInteger last = new CInteger(Integer.MIN_VALUE);
		while (!queue.isEmpty()) {
			j++;
			CInteger current = (CInteger) queue.poll();
			assertTrue(last.getN() < current.getN());
		}
		assertEquals(j, count);
	}
	
	public void testAddPollRandom2() {
		for (int i = 0; i < 1000; i++) {
			queue.add(new CInteger((int) (Math.random() * 1000)));
		}
		
		CInteger last = new CInteger(Integer.MIN_VALUE);
		for (int i = 0; i < 500; i++) {
			CInteger current = (CInteger) queue.poll();
			assertTrue(last.getN() < current.getN());
		}
		
		for (int i = 0; i < 500; i++) {
			queue.add(new CInteger((int) (Math.random() * 1000)));
		}
		
		last = new CInteger(Integer.MIN_VALUE);
		for (int i = 0; i < 500; i++) {
			CInteger current = (CInteger) queue.poll();
			assertTrue(last.getN() < current.getN());
		}
		
		for (int i = 0; i < 500; i++) {
			queue.add(new CInteger((int) (Math.random() * 1000)));
		}
		
		last = new CInteger(Integer.MIN_VALUE);
		for (int i = 0; i < 1000; i++) {
			CInteger current = (CInteger) queue.poll();
			assertTrue(last.getN() < current.getN());
		}
		
		assertTrue(queue.isEmpty());
	}
	
	public void testAddPoll3() {
		ArrayList<CInteger> nums = new ArrayList<CInteger>();
		for (int i = 0; i < 1000; i++) {
			CInteger num = new CInteger((int) (Math.random() * 1000));
			nums.add(num);
			queue.add(num);
		}
		
		for (int i = 0; i < 1000; i++) {
			CInteger num = (CInteger) queue.poll();
			assertTrue(nums.remove(num));
		}
		assertTrue(queue.isEmpty());
		assertTrue(nums.isEmpty());
	}
	
	public void testRemove() {
		assertEquals(null, queue.poll());
		queue.add(new CInteger(5));
		queue.add(new CInteger(-1));
		queue.add(new CInteger(0));
		queue.add(new CInteger(3));
		queue.add(new CInteger(9));
		queue.add(new CInteger(11));
		assertEquals(new CInteger(-1), queue.poll());
		assertEquals(new CInteger(0), queue.poll());
		assertEquals(new CInteger(3), queue.poll());
		queue.add(new CInteger(-22));
		queue.add(new CInteger(100));
		queue.add(new CInteger(6));
		queue.remove(new CInteger(5));
		queue.remove(new CInteger(6));
		assertEquals(new CInteger(-22), queue.poll());
		assertEquals(new CInteger(9), queue.poll());
		assertEquals(new CInteger(11), queue.poll());
		assertEquals(new CInteger(100), queue.poll());
		
		assertTrue(queue.isEmpty());
	}
	
}
