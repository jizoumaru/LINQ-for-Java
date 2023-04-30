package linq;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class CloseCountLinq extends Linq<Integer> {
	public CloseCountLinq(Supplier<? extends Fetch<Integer>> supplier, AtomicInteger closeCount) {
		super(supplier);
		this.closeCount = closeCount;
	}

	public static CloseCountLinq create() {
		var closeCount = new AtomicInteger();
		return new CloseCountLinq(() -> new CloseCountFetch(closeCount), closeCount);
	}

	private final AtomicInteger closeCount;

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
