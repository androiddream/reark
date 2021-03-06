package io.reark.rxgithubapp.advanced.data.stores;

import android.content.pm.ProviderInfo;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reark.rxgithubapp.advanced.data.schematicProvider.generated.GitHubProvider;
import io.reark.rxgithubapp.shared.pojo.GitHubOwner;
import io.reark.rxgithubapp.shared.pojo.GitHubRepository;

import static io.reark.rxgithubapp.advanced.data.schematicProvider.GitHubProvider.GitHubRepositories.GITHUB_REPOSITORIES;
import static io.reark.rxgithubapp.shared.Constants.Tests.PROVIDER_WAIT_TIME;
import static io.reark.rxgithubapp.shared.pojo.GitHubRepository.none;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@RunWith(AndroidJUnit4.class)
public class GitHubRepositoryStoreTest extends ProviderTestCase2<GitHubProvider> {

    private GitHubRepositoryStore gitHubRepositoryStore;

    private TestObserver<GitHubRepository> testObserver;

    private GitHubProvider contentProvider;

    private Gson gson = new Gson();
    
    public GitHubRepositoryStoreTest() {
        super(GitHubProvider.class, GitHubProvider.AUTHORITY);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        setContext(InstrumentationRegistry.getTargetContext());

        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = GitHubProvider.AUTHORITY;

        contentProvider = new GitHubProvider();
        contentProvider.attachInfo(InstrumentationRegistry.getTargetContext(), providerInfo);
        contentProvider.delete(GITHUB_REPOSITORIES, null, null);

        Thread.sleep(PROVIDER_WAIT_TIME);

        final MockContentResolver contentResolver = new MockContentResolver();
        contentResolver.addProvider(GitHubProvider.AUTHORITY, contentProvider);

        gitHubRepositoryStore = new GitHubRepositoryStore(contentResolver, gson);
        testObserver = new TestObserver<>();
        
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        contentProvider.delete(GITHUB_REPOSITORIES, null, null);

        super.tearDown();
    }

    @Test
    public void getOnce_WithId_WithData_ReturnsData_AndCompletes() throws InterruptedException {
        final GitHubRepository value = create(100, "repository1");
        gitHubRepositoryStore.put(value);
        Thread.sleep(PROVIDER_WAIT_TIME);

        // getOnce is expected to return a observable that emits the value and then completes.
        gitHubRepositoryStore.getOnce(100)
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(value);
    }

    @Test
    public void getOnce_WithId_WithNoData_ReturnsNoneValue_AndCompletes() {
        // getOnce is expected to emit empty value in case no actual value exists.
        gitHubRepositoryStore.getOnce(100)
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(none());
    }

    @Test
    public void getOnce_WithNoId_WithData_ReturnsAllData_AndCompletes() throws InterruptedException {
        final GitHubRepository value1 = create(100, "repository1");
        final GitHubRepository value2 = create(200, "repository2");
        gitHubRepositoryStore.put(value1);
        gitHubRepositoryStore.put(value2);
        Thread.sleep(PROVIDER_WAIT_TIME);

        // getOnce with no id is expected to return a observable that emits all values and then completes.
        gitHubRepositoryStore.getOnce()
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(asList(value1, value2));
    }

    @Test
    public void getOnce_WithNoId_WithNoData_ReturnsEmptyList_AndCompletes() {
        // getOnce with no id is expected to emit empty list in case no values exist.
        gitHubRepositoryStore.getOnce()
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(emptyList());
    }

    @Test
    public void getOnceAndStream_ReturnsOnlyValuesForSubscribedId_AndDoesNotComplete() throws InterruptedException {
        final GitHubRepository value1 = create(100, "repository1");
        final GitHubRepository value2 = create(200, "repository2");

        gitHubRepositoryStore.getOnceAndStream(100).subscribeWith(testObserver);
        Thread.sleep(PROVIDER_WAIT_TIME);
        gitHubRepositoryStore.put(value1);
        gitHubRepositoryStore.put(value2);

        testObserver.awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertNotComplete()
                .assertNoErrors()
                .assertValues(none(), value1);
    }

    @Test
    public void getOnceAndStream_ReturnsAllValuesForSubscribedId_AndDoesNotComplete() throws InterruptedException {
        final GitHubRepository value1 = create(100, "repository-1");
        final GitHubRepository value2 = create(100, "repository-2");
        final GitHubRepository value3 = create(100, "repository-3");

        gitHubRepositoryStore.getOnceAndStream(100).subscribeWith(testObserver);
        Thread.sleep(PROVIDER_WAIT_TIME);
        gitHubRepositoryStore.put(value1);
        Thread.sleep(PROVIDER_WAIT_TIME);
        gitHubRepositoryStore.put(value2);
        Thread.sleep(PROVIDER_WAIT_TIME);
        gitHubRepositoryStore.put(value3);

        testObserver.awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertNotComplete()
                .assertNoErrors()
                .assertValues(none(), value1, value2, value3);
    }

    @Test
    public void getOnceAndStream_ReturnsOnlyNewValues_AndDoesNotComplete() throws InterruptedException {
        final GitHubRepository value = create(100, "repository1");

        // In the default store implementation identical values are filtered out.
        gitHubRepositoryStore.getOnceAndStream(100).subscribeWith(testObserver);
        Thread.sleep(PROVIDER_WAIT_TIME);
        gitHubRepositoryStore.put(value);
        gitHubRepositoryStore.put(value);

        testObserver.awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertNotComplete()
                .assertNoErrors()
                .assertValues(none(), value);
    }

    @Test
    public void getOnceAndStream_WithInitialValue_ReturnsInitialValues_AndDoesNotComplete() throws InterruptedException {
        final GitHubRepository value = create(100, "repository1");
        gitHubRepositoryStore.put(value);
        Thread.sleep(PROVIDER_WAIT_TIME);

        gitHubRepositoryStore.getOnceAndStream(100)
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertNotComplete()
                .assertNoErrors()
                .assertValues(value);
    }

    @Test
    public void getOnceAndStream_WithInitialValue_WithDelayedSubscription_ReturnsFirstValue_AndDoesNotComplete() throws InterruptedException {
        final GitHubRepository value1 = create(100, "repository1");
        final GitHubRepository value2 = create(100, "repository1");

        // This behavior is a little surprising, but it is because we cannot guarantee that the
        // observable that is produced as the stream will keep its first (cached) value up to date.
        // The only ways to around this would be custom subscribe function or converting the
        // source observable into a behavior, but these would significantly increase the
        // complexity and are hard to implement in other kinds of store (such as content providers).

        // Put initial value.
        gitHubRepositoryStore.put(value1);
        Thread.sleep(PROVIDER_WAIT_TIME);

        // Create the stream observable but do not subscribe immediately.
        Observable<GitHubRepository> stream = gitHubRepositoryStore.getOnceAndStream(100);

        // Put new value into the store.
        gitHubRepositoryStore.put(value2);

        // Subscribe to stream that was created potentially a long time ago, and observe that the
        // stream actually gives as the first item the cached value at the time of creating the
        // stream observable.
        stream.test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertNotComplete()
                .assertNoErrors()
                .assertValues(value1);
    }

    @Test
    public void put_WithNewData_EmitsTrue() {
        final GitHubRepository value = create(100, "repository1");

        gitHubRepositoryStore.put(value)
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertResult(true);
    }

    @Test
    public void put_WithDifferentData_OverExistingData_EmitsTrue() throws InterruptedException {
        final GitHubRepository value1 = create(100, "repository1");
        final GitHubRepository value2 = create(100, "repository2");
        gitHubRepositoryStore.put(value1);
        Thread.sleep(PROVIDER_WAIT_TIME);

        gitHubRepositoryStore.put(value2)
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertValue(true);
    }

    @Test
    public void put_WithIdenticalData_OverExistingData_EmitsFalse() throws InterruptedException {
        final GitHubRepository value = create(100, "repository1");
        gitHubRepositoryStore.put(value);
        Thread.sleep(PROVIDER_WAIT_TIME);

        gitHubRepositoryStore.put(value)
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertValue(false);
    }

    @Test
    public void delete_WithNoData_EmitsFalse() {
        gitHubRepositoryStore.delete(765)
                .test()
                .awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertComplete()
                .assertValue(false);
    }

    @Test
    public void delete_WithData_DeletesData_AndEmitsTrue() throws InterruptedException {
        final GitHubRepository value = create(100, "repository1");
        gitHubRepositoryStore.put(value);
        Thread.sleep(PROVIDER_WAIT_TIME);

        TestObserver<Boolean> ts1 = gitHubRepositoryStore.delete(100).test();
        Thread.sleep(PROVIDER_WAIT_TIME);
        TestObserver<GitHubRepository> ts2 = gitHubRepositoryStore.getOnce(100).test();

        ts1.awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertComplete()
                .assertValue(true);
        ts2.awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValues(none());
    }

    @Test
    public void getOnceAndStream_ThenDelete_DoesNotEmit() throws InterruptedException {
        final GitHubRepository value = create(100, "repository1");
        gitHubRepositoryStore.put(value);
        Thread.sleep(PROVIDER_WAIT_TIME);

        TestObserver<GitHubRepository> ts = gitHubRepositoryStore.getOnceAndStream(100).test();
        gitHubRepositoryStore.delete(100);

        ts.awaitDone(PROVIDER_WAIT_TIME, TimeUnit.MILLISECONDS)
                .assertNotComplete()
                .assertNoErrors()
                .assertValues(value);
    }

    @NonNull
    private static GitHubRepository create(int id, String name) {
        return new GitHubRepository(id, name, 10, 10, new GitHubOwner("owner"));
    }
}
