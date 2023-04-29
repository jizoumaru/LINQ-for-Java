package linq;

import java.util.concurrent.atomic.AtomicInteger;

public class CloseCountLinq implements Linq<Integer> {
	private final AtomicInteger closeCount = new AtomicInteger(0);

	@Override
	public Fetch<Integer> fetch() {
		return new CloseCountFetch(closeCount);
	}

	public int getCloseCount() {
		return closeCount.get();
	}

	static class CloseCountFetch extends Fetch<Integer> {
		private final AtomicInteger closeCount;
		private boolean terminated = false;

		CloseCountFetch(AtomicInteger closeCount) {
			this.closeCount = closeCount;
		}

		@Override
		protected Holder<Integer> internalNext() {
			if (!terminated) {
				terminated = true;
				return Holder.of(1);
			}
			return Holder.none();
		}

		@Override
		protected void internalClose() {
			closeCount.incrementAndGet();
		}
	}
}
