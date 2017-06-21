import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.Observable;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class ReactiveTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testCopyListWithBoringIterators() {
		final List<String> list = Arrays.asList("a", "b");
		final List<String> target = new ArrayList<>();

		Iterator<String> iterator = list.iterator();

		while (iterator.hasNext()) {
			target.add(iterator.next());
		}

		assertThat(target, hasItems("a", "b"));
	}

	@Test
	public void testJavaStream() {
		List<String> outcome = new ArrayList<>();
		Arrays.asList("a", "b").stream().forEach(outcome::add);

		assertThat(outcome, hasItems("a", "b"));
	}

	@Test
	public void testObservableJust() {
		Observable<Integer> observable = Observable.just(1, 2, 3);
		AtomicInteger counter = new AtomicInteger();

		observable.subscribe(counter::getAndAdd);

		assertEquals(counter.get(), 6);
	}

	@Test
	public void testObservableJustWithDoneHandler() {
		Observable<Integer> observable = Observable.just(1, 2, 3);
		AtomicInteger counter = new AtomicInteger();

		observable.subscribe(counter::getAndAdd, Exceptions::propagate, counter::getAndIncrement);

		assertEquals(counter.get(), 7);
	}

	@Test
	public void testObservableJustWithOnNextHandler() throws Exception {
		Observable<Integer> observable = Observable.just(1, 2, 3);
		AtomicInteger counter = new AtomicInteger();

		observable.doOnNext(counter::getAndAdd).subscribe();

		assertEquals(counter.get(), 6);
	}

	@Test
	public void testObservableSendsToMultipleSubscribers() throws Exception {
		final CountDownLatch latch = new CountDownLatch(6);
		final Observable<Integer> observable = Observable.just(1, 1, 1);

		observable.subscribe((i) -> latch.countDown());
		observable.subscribe((i) -> latch.countDown());

		latch.await();
	}


	@Test
	public void testObservableInterval() throws Exception {
		final AtomicLong counter = new AtomicLong();

		Observable.interval(100L, TimeUnit.MILLISECONDS).subscribe(counter::getAndAdd, System.err::println);

		Thread.sleep(950L);

		assertEquals(counter.get(), 0 + 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8);
	}

	@Test
	public void testObservableEmptyCallsOnlyDoneHandler() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Observable
				.empty()
				.subscribe(
						(l) -> Assert.fail(),
						(err) -> Assert.fail(),
						() -> latch.countDown()
				);

		latch.await();
	}

	@Test
	public void testObservableErrorCallsOnlyErrorHandler() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);

		Observable
				.error(new Exception("boom"))
				.subscribe(
						(l) -> Assert.fail(),
						(err) -> latch.countDown(),
						() -> Assert.fail()
				);

		latch.await();
	}

	@Test
	public void testObservableNeverCallsNothing() throws Exception {
		Observable
				.never()
				.subscribe(
						(e) -> Assert.fail(),
						(err) -> Assert.fail(),
						() -> Assert.fail()
				);

		Thread.sleep(100);
	}

	@Test
	public void testObservableRange() throws Exception {
		final AtomicInteger counter = new AtomicInteger();
		Observable.
				range(0, 6)
				.subscribe(counter::getAndAdd);

		assertEquals(0 + 1 + 2 + 3 + 4 + 5, counter.get());
	}

	@Test
	public void testObservableThrowsExceptionIfNoErrorHandlerExists() {
		thrown.expect(OnErrorNotImplementedException.class);
		thrown.expectMessage("nope");
		Observable.error(new Exception("nope")).subscribe();
	}

	@Test
	public void testCreateObservableForIterable() {
		final List<String> cars = Arrays.asList("audi", "tesla");
		final List<String> capitalizedCars = new ArrayList<>();
		final Func1<String, String> capitalize = s -> s.substring(0, 1).toUpperCase() + s.substring(1);

		fromIterable(cars)
				.map(capitalize)
				.subscribe(
						capitalizedCars::add,
						err -> Assert.fail()
				);

		assertThat(capitalizedCars, hasItems("Audi", "Tesla"));
	}

	private <T> Observable<T> fromIterable(final Iterable<T> iterable) {
		return Observable.create(subscriber -> {

			try {
				Iterator<T> iterator = iterable.iterator();

				while (iterator.hasNext()) {
					subscriber.onNext(iterator.next());
				}
				subscriber.onCompleted();
			} catch (Exception e) {
				subscriber.onError(e);
			}

		});
	}


}
